package com.kungfuchess.view;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.engine.MoveRecord;
import com.kungfuchess.input.Controller;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * The Swing shell: one resizable window, one image label repainted from a {@link Timer}, a
 * move-history side panel, and one mouse listener. It never draws the board itself -- every
 * pixel it shows there comes from {@link ImgRenderer#render}, scaled (aspect preserved) to
 * whatever size the user has resized the window to. Every click it receives is translated back
 * from displayed pixels to the renderer's native pixel space before being handed to the existing
 * {@link Controller}, unchanged from how the text protocol already drives it.
 */
public final class GameWindow {

    private static final int FRAME_DELAY_MS = 16;
    private static final Color LETTERBOX_COLOR = new Color(20, 20, 20);
    private static final int MOVE_LIST_WIDTH_PX = 220;

    private final GameEngine engine;
    private final Controller controller;
    private final ImgRenderer renderer;

    private JFrame frame;
    private JLabel imageLabel;
    private DefaultListModel<String> whiteMoveListModel;
    private DefaultListModel<String> blackMoveListModel;
    private JList<String> whiteMoveList;
    private JList<String> blackMoveList;
    private Timer timer;
    private long previousTimeNanos;
    private int renderedMoveCount;

    // How the most recently rendered native-resolution frame currently maps onto the label:
    // scaled (with letterboxing) to whatever size the user has resized the window to.
    private int nativeWidth;
    private int nativeHeight;
    private int displayedWidth;
    private int displayedHeight;
    private int letterboxOffsetX;
    private int letterboxOffsetY;

    public GameWindow(GameEngine engine, Controller controller, ImgRenderer renderer) {
        this.engine = engine;
        this.controller = controller;
        this.renderer = renderer;
    }

    public void start() {
        SwingUtilities.invokeLater(() -> {
            createWindow();
            startGameLoop();
        });
    }

    private void createWindow() {
        frame = new JFrame("Kung Fu Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);

        imageLabel = new JLabel();
        imageLabel.setOpaque(true);
        imageLabel.setBackground(LETTERBOX_COLOR);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                handleClick(event);
            }
        });

        whiteMoveListModel = new DefaultListModel<>();
        whiteMoveList = new JList<>(whiteMoveListModel);
        whiteMoveList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane whiteScrollPane = new JScrollPane(whiteMoveList);
        whiteScrollPane.setBorder(BorderFactory.createTitledBorder("White"));
        whiteScrollPane.setPreferredSize(new Dimension(MOVE_LIST_WIDTH_PX, 0));

        blackMoveListModel = new DefaultListModel<>();
        blackMoveList = new JList<>(blackMoveListModel);
        blackMoveList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane blackScrollPane = new JScrollPane(blackMoveList);
        blackScrollPane.setBorder(BorderFactory.createTitledBorder("Black"));
        blackScrollPane.setPreferredSize(new Dimension(MOVE_LIST_WIDTH_PX, 0));

        frame.add(imageLabel, BorderLayout.CENTER);
        frame.add(blackScrollPane, BorderLayout.WEST);
        frame.add(whiteScrollPane, BorderLayout.EAST);
        renderCurrentState();

        frame.pack();
        frame.setMinimumSize(new Dimension(300 + 2 * MOVE_LIST_WIDTH_PX, 300));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void handleClick(MouseEvent event) {
        int localX = event.getX() - letterboxOffsetX;
        int localY = event.getY() - letterboxOffsetY;
        if (localX < 0 || localY < 0 || localX >= displayedWidth || localY >= displayedHeight) {
            return; // clicked in the letterbox margin, not on the rendered board at all
        }

        int nativeX = (int) (localX * (nativeWidth / (double) displayedWidth));
        int nativeY = (int) (localY * (nativeHeight / (double) displayedHeight));

        // The board is drawn below the top score bar, so board row 0 sits at pixel
        // SCORE_BAR_HEIGHT_PX, not 0 -- clicks must be shifted back before they reach BoardMapper,
        // or every click would be interpreted one score-bar-height too low.
        int boardY = nativeY - GameConfig.SCORE_BAR_HEIGHT_PX;

        if (event.getButton() == MouseEvent.BUTTON3) {
            controller.handleJump(nativeX, boardY);
        } else {
            controller.handleClick(nativeX, boardY);
        }
    }

    private void startGameLoop() {
        previousTimeNanos = System.nanoTime();
        timer = new Timer(FRAME_DELAY_MS, event -> updateFrame());
        timer.start();
    }

    private void updateFrame() {
        long now = System.nanoTime();
        long elapsedMillis = (now - previousTimeNanos) / 1_000_000;
        previousTimeNanos = now;

        controller.waitClock(elapsedMillis);
        renderCurrentState();
    }

    private void renderCurrentState() {
        GameSnapshot snapshot = engine.snapshot(controller.getSelectedPosition());

        Img rendered = renderer.render(snapshot);
        nativeWidth = rendered.get().getWidth();
        nativeHeight = rendered.get().getHeight();

        int targetWidth = imageLabel.getWidth();
        int targetHeight = imageLabel.getHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            // Not yet laid out (first render, before the initial pack()) -- show at native size.
            targetWidth = nativeWidth;
            targetHeight = nativeHeight;
        }

        Img scaled = rendered.scaledTo(targetWidth, targetHeight, true);
        displayedWidth = scaled.get().getWidth();
        displayedHeight = scaled.get().getHeight();
        letterboxOffsetX = (targetWidth - displayedWidth) / 2;
        letterboxOffsetY = (targetHeight - displayedHeight) / 2;

        imageLabel.setIcon(new ImageIcon(scaled.get()));
        imageLabel.repaint();

        appendNewMoves(snapshot);
    }

    private void appendNewMoves(GameSnapshot snapshot) {
        List<MoveRecord> history = snapshot.moveHistory();
        if (history.size() <= renderedMoveCount) {
            return;
        }

        for (int i = renderedMoveCount; i < history.size(); i++) {
            MoveRecord record = history.get(i);
            String formatted = formatMove(record, snapshot.boardRows());
            if (record.color() == PieceColor.WHITE) {
                whiteMoveListModel.addElement(formatted);
                whiteMoveList.ensureIndexIsVisible(whiteMoveListModel.getSize() - 1);
            } else {
                blackMoveListModel.addElement(formatted);
                blackMoveList.ensureIndexIsVisible(blackMoveListModel.getSize() - 1);
            }
        }
        renderedMoveCount = history.size();
    }

    private String formatMove(MoveRecord record, int boardRows) {
        String time = formatElapsed(record.timestampMs());
        String kindName = capitalize(record.kind());
        String from = algebraic(record.source(), boardRows);

        if (record.isJump()) {
            return time + "  " + kindName + " jump " + from;
        }
        String to = algebraic(record.destination(), boardRows);
        return time + "  " + kindName + " " + from + "-" + to;
    }

    private String algebraic(Position position, int boardRows) {
        char file = (char) ('a' + position.getCol());
        int rank = boardRows - position.getRow();
        return "" + file + rank;
    }

    private String formatElapsed(long ms) {
        long totalSeconds = ms / 1000;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private String capitalize(PieceKind kind) {
        String name = kind.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
