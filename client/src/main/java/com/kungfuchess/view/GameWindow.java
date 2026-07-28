package com.kungfuchess.view;

import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.engine.MoveRecord;
import com.kungfuchess.engine.TransientEvent;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.net.LoginResponse;
import com.kungfuchess.net.MatchResult;
import com.kungfuchess.net.RejectionNotice;
import com.kungfuchess.net.RoomJoinResult;
import com.kungfuchess.net.RoomRole;
import com.kungfuchess.net.SelectionController;
import com.kungfuchess.net.ServerConnection;
import com.kungfuchess.sound.SoundEffect;
import com.kungfuchess.sound.SoundPlayer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * The Swing shell -- networked. Unlike the original local version, there is no {@code Timer}
 * driving a clock here: the server owns the game clock now ({@code GameClock}, ticking every
 * 16ms), and this class simply re-renders whenever {@link ServerConnection} hands it a fresh
 * {@link GameSnapshot} pushed over the wire. Every click is translated from displayed pixels to
 * the renderer's native pixel space (via {@link ScreenToBoardMapper}) before being handed to
 * {@link SelectionController}, which resolves selection locally and sends a command to the server
 * -- this class never touches game rules or an engine directly.
 */
public final class GameWindow {

    private static final Color LETTERBOX_COLOR = new Color(20, 20, 20);
    private static final int STANDARD_BOARD_ROWS = 8; // matches StandardBoard; no variable board size exists yet

    private final ServerConnection connection;
    private final SelectionController selectionController;
    private final ImgRenderer renderer;
    private final SoundPlayer soundPlayer;

    private JFrame frame;
    private JLabel imageLabel;
    private MoveHistoryPanel moveHistoryPanel;

    private GameSnapshot latestSnapshot;
    private boolean gameOverSoundPlayed;

    // Coalesces bursty snapshot delivery: only the newest snapshot is ever queued for rendering,
    // so a backlog on the EDT can never build up (and stall mouse clicks) behind a pile of stale
    // frames -- see the class doc on why this matters now that a server can broadcast several
    // rooms' worth of traffic.
    private volatile GameSnapshot pendingSnapshot;
    private final AtomicBoolean renderScheduled = new AtomicBoolean(false);

    // How the most recently rendered native-resolution frame currently maps onto the label:
    // scaled (with letterboxing) to whatever size the user has resized the window to.
    private int nativeWidth;
    private int nativeHeight;
    private int displayedWidth;
    private int displayedHeight;
    private int letterboxOffsetX;
    private int letterboxOffsetY;

    public GameWindow(ServerConnection connection, SelectionController selectionController,
                       ImgRenderer renderer, SoundPlayer soundPlayer) {
        this.connection = connection;
        this.selectionController = selectionController;
        this.renderer = renderer;
        this.soundPlayer = soundPlayer;
    }

    /**
     * Builds and shows the window immediately (fast, nothing to wait on), then connects, logs in,
     * and enters a game on the calling thread -- all block briefly, and must not run on the EDT or
     * they'd freeze the not-yet-visible window while they wait. The window is fully usable before
     * any of that finishes; there's just nothing to click yet ({@link SelectionController} won't
     * select anything without a color), matching how the board itself won't have rendered until
     * the first snapshot arrives either.
     *
     * <p>On a wrong password, {@code passwordPrompt} is invoked again (e.g. to re-prompt on the
     * console) and login is retried with the same username. Once logged in (and there's no
     * reclaimed seat -- see below), {@link LobbyDialog} is shown so the player picks between quick
     * matchmaking ("Play") and a code-based room ("Room"); a failed search or room join re-shows
     * the same dialog so retrying is always an explicit choice.
     *
     * <p>If login reports a reclaimed seat -- this account reconnecting within its 20-second
     * disconnect grace period (see {@code DisconnectResignHandler}) -- the lobby is skipped
     * entirely and that seat's room is resumed directly.
     */
    public void start(String serverUrl, String username, Supplier<String> passwordPrompt) {
        SwingUtilities.invokeLater(this::createWindow);
        connection.connect(serverUrl, this::onSnapshotReceived, this::onRejectionReceived);
        soundPlayer.play(SoundEffect.GAME_START);

        LoginResponse loginResponse = connection.login(username, passwordPrompt.get());
        while (!loginResponse.success()) {
            System.out.println("Login failed: " + loginResponse.errorMessage());
            loginResponse = connection.login(username, passwordPrompt.get());
        }
        int rating = loginResponse.rating();
        System.out.println("Logged in as " + username + " (rating " + rating + ")");

        MatchResult reclaimed = loginResponse.reclaimedSeat();
        if (reclaimed != null) {
            System.out.println("Reconnected -- resuming as " + reclaimed.color());
            enterAsPlayer(reclaimed.roomId(), reclaimed.color(), username, rating,
                    describeOpponent(reclaimed.opponentUsername(), reclaimed.opponentRating()));
            return;
        }

        enterLobby(username, rating);
    }

    /** Loops between the lobby dialog and a search/join attempt until one of them succeeds. */
    private void enterLobby(String username, int rating) {
        while (true) {
            LobbyDialog.Choice choice = LobbyDialog.showAndGetChoice();
            if (choice == LobbyDialog.Choice.PLAY) {
                System.out.println("Searching for an opponent (rating +/-100, 60s timeout)...");
                MatchResult matchResult = connection.play();
                if (!matchResult.matched()) {
                    System.out.println("No match: " + matchResult.failureReason());
                    continue;
                }
                enterAsPlayer(matchResult.roomId(), matchResult.color(), username, rating,
                        describeOpponent(matchResult.opponentUsername(), matchResult.opponentRating()));
                return;
            }

            String roomCode = LobbyDialog.promptForRoomCode();
            if (roomCode == null || roomCode.isEmpty()) {
                continue; // cancelled -- back to the lobby dialog
            }
            RoomJoinResult roomResult = connection.joinRoom(roomCode);
            if (!roomResult.success()) {
                System.out.println("Could not join room '" + roomCode + "': " + roomResult.failureReason());
                continue;
            }
            enterRoom(roomResult, username, rating);
            return;
        }
    }

    private void enterRoom(RoomJoinResult roomResult, String username, int rating) {
        if (roomResult.role() == RoomRole.SPECTATOR) {
            connection.subscribeToRoom(roomResult.roomId());
            selectionController.setMyColor(null);
            SwingUtilities.invokeLater(() -> updateTitle(
                    "Spectating " + roomResult.whiteUsername() + " vs " + roomResult.blackUsername()));
            return;
        }

        PieceColor color = roomResult.role() == RoomRole.WHITE ? PieceColor.WHITE : PieceColor.BLACK;
        String opponentUsername = color == PieceColor.WHITE ? roomResult.blackUsername() : roomResult.whiteUsername();
        String opponentDescription = opponentUsername == null ? "waiting for an opponent" : opponentUsername;
        enterAsPlayer(roomResult.roomId(), color, username, rating, opponentDescription);
    }

    private void enterAsPlayer(String roomId, PieceColor color, String username, int rating,
            String opponentDescription) {
        connection.subscribeToRoom(roomId);
        selectionController.setMyColor(color);
        SwingUtilities.invokeLater(() -> updateTitle(
                username + " (playing " + color.name() + ", rating " + rating + ") vs " + opponentDescription));
    }

    private String describeOpponent(String opponentUsername, int opponentRating) {
        return opponentUsername + " (rating " + opponentRating + ")";
    }

    private void updateTitle(String description) {
        frame.setTitle("Kung Fu Chess - " + description);
    }

    /** Called on a background (STOMP client) thread, same as {@link #onSnapshotReceived}. */
    private void onRejectionReceived(RejectionNotice notice) {
        SwingUtilities.invokeLater(() -> soundPlayer.play(SoundEffect.REJECTED));
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

        moveHistoryPanel = new MoveHistoryPanel(STANDARD_BOARD_ROWS);

        frame.add(imageLabel, BorderLayout.CENTER);
        frame.add(moveHistoryPanel.blackPanel(), BorderLayout.WEST);
        frame.add(moveHistoryPanel.whitePanel(), BorderLayout.EAST);

        frame.pack();
        frame.setMinimumSize(new Dimension(300 + 2 * MoveHistoryPanel.MOVE_LIST_WIDTH_PX, 300));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Called on a background (STOMP client) thread -- everything here must marshal to the EDT.
     * Only ever schedules a single pending {@code invokeLater} at a time: if several snapshots
     * arrive before the EDT gets to the last one, {@link #pendingSnapshot} is simply overwritten
     * and the newest value wins once the already-queued render runs -- {@link #processNewMoves}
     * and {@link #playCaptureAndGameEndSounds} both diff against whatever was last actually
     * rendered (not "the previous broadcast"), so skipping intermediate snapshots this way loses
     * no moves/captures/game-end detection, only stale intermediate paint frames nobody would have
     * seen anyway.
     */
    private void onSnapshotReceived(GameSnapshot snapshot) {
        pendingSnapshot = snapshot;
        if (renderScheduled.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(this::renderPendingSnapshot);
        }
    }

    /** Runs on the EDT. */
    private void renderPendingSnapshot() {
        renderScheduled.set(false);
        GameSnapshot snapshot = pendingSnapshot;

        processNewMoves(latestSnapshot, snapshot);
        playCaptureAndGameEndSounds(latestSnapshot, snapshot);
        playTransientEventSounds(snapshot);
        latestSnapshot = snapshot;
        renderCurrentState();
    }

    /**
     * Unlike move/capture (inferred by diffing), the server reports these explicitly on the
     * snapshot itself -- see {@link TransientEvent}'s own doc for why that split exists.
     */
    private void playTransientEventSounds(GameSnapshot snapshot) {
        for (TransientEvent event : snapshot.events()) {
            if (event == TransientEvent.PAWN_PROMOTED) {
                soundPlayer.play(SoundEffect.PROMOTION);
            }
        }
    }

    /**
     * Feeds every newly appeared {@link MoveRecord} (there may be more than one between two
     * broadcasts) into the move-history panel and plays its move/jump sound -- the networked
     * equivalent of the local version's {@code engine.addMoveListener}, except this reacts to
     * both players' moves, not just clicks made in this window.
     */
    private void processNewMoves(GameSnapshot previous, GameSnapshot current) {
        int previousSize = previous == null ? 0 : previous.moveHistory().size();
        List<MoveRecord> history = current.moveHistory();
        for (int i = previousSize; i < history.size(); i++) {
            MoveRecord record = history.get(i);
            moveHistoryPanel.onMoveRecorded(record);
            soundPlayer.play(record.isJump() ? SoundEffect.JUMP : SoundEffect.MOVE);
        }
    }

    /**
     * Capture and game-end have no per-event push like moves do, so both are detected by diffing
     * consecutive snapshots: a capture is exactly a drop in total piece count (decoupled from
     * request timing, unlike the local version's "occupied at request time" check -- a capture
     * here is only signaled once the server has actually resolved it), and game-end is the
     * gameOver flag's false-to-true edge, same as the local version.
     */
    private void playCaptureAndGameEndSounds(GameSnapshot previous, GameSnapshot current) {
        if (previous != null && current.pieces().size() < previous.pieces().size()) {
            soundPlayer.play(SoundEffect.CAPTURE);
        }
        if ((previous == null || !previous.gameOver()) && current.gameOver() && !gameOverSoundPlayed) {
            gameOverSoundPlayed = true;
            soundPlayer.play(SoundEffect.GAME_END);
        }
    }

    private void handleClick(MouseEvent event) {
        if (latestSnapshot == null) {
            return; // no data from the server yet
        }

        ScreenToBoardMapper.Frame screenFrame = new ScreenToBoardMapper.Frame(
                nativeWidth, nativeHeight, displayedWidth, displayedHeight, letterboxOffsetX, letterboxOffsetY);
        ScreenToBoardMapper.BoardPixel pixel = ScreenToBoardMapper.toBoardPixel(event.getX(), event.getY(), screenFrame);
        if (pixel == null) {
            return; // clicked in the letterbox margin, not on the rendered board at all
        }

        if (event.getButton() == MouseEvent.BUTTON3) {
            selectionController.handleJump(pixel.x(), pixel.y());
        } else {
            selectionController.handleClick(pixel.x(), pixel.y(), latestSnapshot);
        }
        renderCurrentState(); // selection state changed locally; reflect it before the next broadcast arrives
    }

    private void renderCurrentState() {
        if (latestSnapshot == null || imageLabel == null) {
            return;
        }

        GameSnapshot augmented = new GameSnapshot(
                latestSnapshot.boardRows(), latestSnapshot.boardCols(), latestSnapshot.pieces(),
                selectionController.getSelectedPosition(), latestSnapshot.gameOver(),
                latestSnapshot.whiteScore(), latestSnapshot.blackScore(), latestSnapshot.winner(),
                latestSnapshot.moveHistory(), selectionController.legalDestinations(latestSnapshot),
                latestSnapshot.events());

        Img rendered = renderer.render(augmented);
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
    }
}
