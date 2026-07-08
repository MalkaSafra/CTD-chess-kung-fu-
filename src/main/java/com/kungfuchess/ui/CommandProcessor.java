package com.kungfuchess.ui;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.Game;

/**
 * Parses one command line at a time and delegates to {@link Game}'s API.
 *
 * <p>This class knows the text syntax of each command (its keyword and
 * argument count) and nothing about how the game itself works -- it never
 * touches a {@code Board}, a queue, or a clock directly. That split keeps
 * "how do we read a command" and "what does the game do with it" as two
 * independently changeable responsibilities.
 */
public final class CommandProcessor {

    private final Game game;

    public CommandProcessor(Game game) {
        this.game = game;
    }

    public void process(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String[] parts = trimmed.split("\\s+");
        switch (parts[0].toLowerCase()) {
            case GameConfig.COMMAND_CLICK:
                handleClick(parts);
                break;
            case GameConfig.COMMAND_JUMP:
                handleJump(parts);
                break;
            case GameConfig.COMMAND_WAIT:
                handleWait(parts);
                break;
            case GameConfig.COMMAND_PRINT:
                handlePrint(parts);
                break;
            default:
                break;
        }
    }

    private void handleClick(String[] parts) {
        int[] xy = parseXY(parts);
        if (xy != null) {
            game.handleClick(xy[0], xy[1]);
        }
    }

    private void handleJump(String[] parts) {
        int[] xy = parseXY(parts);
        if (xy != null) {
            game.handleJump(xy[0], xy[1]);
        }
    }

    private int[] parseXY(String[] parts) {
        if (parts.length < 3) {
            return null;
        }
        try {
            return new int[] { Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleWait(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        try {
            game.waitClock(Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            // Malformed wait amount: ignored, same as any other unparsable command.
        }
    }

    private void handlePrint(String[] parts) {
        if (parts.length >= 2 && parts[1].equalsIgnoreCase(GameConfig.PRINT_ARG_BOARD)) {
            game.printBoard();
        }
    }
}
