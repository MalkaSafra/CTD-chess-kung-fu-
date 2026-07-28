package com.kungfuchess.view;

import javax.swing.JOptionPane;

/**
 * The "how do you want to start a game" prompt shown after login: a real Swing dialog with a
 * "Play" button (ELO+/-100 quick matchmaking) and a "Room" button (opens a second dialog to type
 * a room code -- entering a new code creates that room, entering an existing one joins it, and
 * joining a room that already has two seated players makes you a spectator). Deliberately a plain
 * {@link JOptionPane} rather than a hand-built {@code JDialog}: {@code GameWindow} already calls
 * this from the same background thread that blocks on the login/matchmaking network calls (not the
 * EDT), and {@code JOptionPane}'s static helpers are safe to call that way -- each one builds and
 * shows its own modal dialog and blocks the calling thread until the player responds, the same
 * blocking-prompt pattern the console username/password prompts already use.
 */
public final class LobbyDialog {

    public enum Choice {
        PLAY, ROOM
    }

    private static final String PLAY_OPTION = "Play (Quick Match)";
    private static final String ROOM_OPTION = "Room";

    private LobbyDialog() {
    }

    /** Blocks until the player picks Play or Room. */
    public static Choice showAndGetChoice() {
        Object[] options = {PLAY_OPTION, ROOM_OPTION};
        int selection = JOptionPane.showOptionDialog(null, "Choose how to start a game", "Kung Fu Chess",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        return selection == 1 ? Choice.ROOM : Choice.PLAY;
    }

    /** Blocks until the player types a code and confirms, or cancels ({@code null}/blank). */
    public static String promptForRoomCode() {
        String code = JOptionPane.showInputDialog(null, "Enter a room code (existing or new):",
                "Join / Create Room", JOptionPane.PLAIN_MESSAGE);
        return code == null ? null : code.trim();
    }
}
