package com.kungfuchess.app;

import com.kungfuchess.net.SelectionController;
import com.kungfuchess.net.ServerConnection;
import com.kungfuchess.sound.SoundPlayer;
import com.kungfuchess.view.GameWindow;
import com.kungfuchess.view.ImgRenderer;
import com.kungfuchess.view.PieceImageLoader;

import java.io.Console;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Composition root for the GUI entry point: only builds objects and wires their dependencies,
 * mirroring the existing text-mode {@link com.kungfuchess.Main} -- neither one contains any game
 * logic of its own. Networked: there is no local {@code GameEngine} here at all -- the server
 * (see {@code com.kungfuchess.server.ServerApplication}) owns the one authoritative game; this
 * process only renders what it's told and sends the moves the user clicks.
 *
 * <p>Login is a console prompt, not a GUI screen, per the deck ("do it in a shell, not via GUI")
 * -- it's the one piece of the "Home screen" this stage actually calls for. A first login for a
 * username auto-registers the account; a repeat login must match the stored password, and {@link
 * GameWindow#start} re-prompts (via {@link #readPassword}) on a rejection. Choosing how to start a
 * game -- quick matchmaking ("Play") or a code-based room ("Room") -- is a real Swing dialog
 * instead (see {@code LobbyDialog}), shown by {@code GameWindow} itself once login succeeds.
 */
public final class GuiMain {

    private static final String DEFAULT_SERVER_URL = "ws://localhost:8080/ws";

    public static void main(String[] args) {
        String serverUrl = args.length > 0 ? args[0] : DEFAULT_SERVER_URL;

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();

        Path piecesRoot = Path.of("pieces_classic");

        ServerConnection connection = new ServerConnection();
        SelectionController selectionController = new SelectionController(connection);

        PieceImageLoader imageLoader = new PieceImageLoader(piecesRoot);
        ImgRenderer renderer = new ImgRenderer("board_classic.png", imageLoader);
        SoundPlayer soundPlayer = new SoundPlayer(Path.of("sounds"));
        GameWindow window = new GameWindow(connection, selectionController, renderer, soundPlayer);

        window.start(serverUrl, username, () -> readPassword(scanner));
    }

    /**
     * Masked input via the real console when one is attached (normal interactive use); a plain
     * {@link Scanner} line read otherwise -- there is no real console when stdin is piped or
     * redirected, which is how this entry point is driven for automated verification.
     */
    private static String readPassword(Scanner fallbackScanner) {
        Console console = System.console();
        if (console != null) {
            return new String(console.readPassword("Enter your password: "));
        }
        System.out.print("Enter your password: ");
        return fallbackScanner.nextLine();
    }
}
