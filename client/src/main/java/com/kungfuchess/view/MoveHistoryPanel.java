package com.kungfuchess.view;

import com.kungfuchess.engine.MoveRecord;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * The two side-by-side move-history tables (one per color) that {@link GameWindow} places at its
 * west/east edges, and the logic that turns a recorded {@link MoveRecord} into a formatted row in
 * the right one -- separated out of {@code GameWindow} so the window shell itself is only about
 * the board image and the game loop.
 */
final class MoveHistoryPanel {

    static final int MOVE_LIST_WIDTH_PX = 220;

    private static final Color TITLE_BACKGROUND = new Color(70, 70, 70);
    private static final Color HEADER_BACKGROUND = new Color(55, 55, 55);
    private static final Color HEADER_TEXT_COLOR = new Color(200, 200, 200);
    private static final Color PANEL_BACKGROUND = new Color(30, 30, 30);
    private static final Color GRID_COLOR = new Color(50, 50, 50);
    private static final int TITLE_FONT_SIZE = 16;
    private static final int MOVE_FONT_SIZE = 14;
    private static final int MOVE_ROW_HEIGHT_PX = 22;

    private final int boardRows;
    private final DefaultTableModel whiteMoveTableModel;
    private final DefaultTableModel blackMoveTableModel;
    private final JTable whiteMoveTable;
    private final JTable blackMoveTable;
    private final JPanel whitePanel;
    private final JPanel blackPanel;

    MoveHistoryPanel(int boardRows) {
        this.boardRows = boardRows;

        whiteMoveTable = newMoveTable();
        whiteMoveTableModel = (DefaultTableModel) whiteMoveTable.getModel();
        whitePanel = wrapWithTitle("White", whiteMoveTable);

        blackMoveTable = newMoveTable();
        blackMoveTableModel = (DefaultTableModel) blackMoveTable.getModel();
        blackPanel = wrapWithTitle("Black", blackMoveTable);
    }

    JPanel whitePanel() {
        return whitePanel;
    }

    JPanel blackPanel() {
        return blackPanel;
    }

    /**
     * Registered with {@link com.kungfuchess.engine.GameEngine#addMoveListener} instead of being
     * discovered by polling each frame: a recorded move is a discrete event, so the engine can
     * push it the moment it happens rather than this panel having to notice it grew.
     */
    void onMoveRecorded(MoveRecord record) {
        String time = formatElapsed(record.timestampMs());
        String move = moveText(record);

        DefaultTableModel model = record.color() == PieceColor.WHITE ? whiteMoveTableModel : blackMoveTableModel;
        JTable table = record.color() == PieceColor.WHITE ? whiteMoveTable : blackMoveTable;

        model.addRow(new Object[] {time, move});
        int lastRow = model.getRowCount() - 1;
        table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
    }

    /** A non-editable, non-interactive two-column ("Time", "Move") table, styled for the dark theme. */
    private JTable newMoveTable() {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Time", "Move"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setEnabled(false);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, MOVE_FONT_SIZE));
        table.setRowHeight(MOVE_ROW_HEIGHT_PX);
        table.setForeground(Color.WHITE);
        table.setBackground(PANEL_BACKGROUND);
        table.setGridColor(GRID_COLOR);
        table.setShowVerticalLines(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, MOVE_FONT_SIZE));
        header.setForeground(HEADER_TEXT_COLOR);
        header.setBackground(HEADER_BACKGROUND);
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);

        return table;
    }

    /** Wraps {@code table} with the bold color-name title bar shown above it. */
    private JPanel wrapWithTitle(String title, JTable table) {
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(TITLE_BACKGROUND);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(PANEL_BACKGROUND);
        scrollPane.setPreferredSize(new Dimension(MOVE_LIST_WIDTH_PX, 0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private String moveText(MoveRecord record) {
        String kindName = capitalize(record.kind());
        String from = algebraic(record.source());

        if (record.isJump()) {
            return kindName + " jump " + from;
        }
        String to = algebraic(record.destination());
        return kindName + " " + from + "-" + to;
    }

    private String algebraic(Position position) {
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
