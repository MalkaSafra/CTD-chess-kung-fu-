package com.kungfuchess.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import com.kungfuchess.model.Board;

/**
 * The standard chess starting position, ready to use -- shared by every composition root
 * (headless, GUI, server) that needs to start a game from scratch, so the position text exists
 * in exactly one place.
 */
public final class StandardBoard {

    private static final String STARTING_POSITION = """
            Board:
            bR bN bB bQ bK bB bN bR
            bP bP bP bP bP bP bP bP
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            wP wP wP wP wP wP wP wP
            wR wN wB wQ wK wB wN wR
            """;

    private StandardBoard() {
    }

    public static Board create() {
        try (BufferedReader reader = new BufferedReader(new StringReader(STARTING_POSITION))) {
            return BoardParser.parse(reader).getBoard();
        } catch (IOException | BoardParseException e) {
            throw new IllegalStateException("Built-in starting position failed to parse", e);
        }
    }
}
