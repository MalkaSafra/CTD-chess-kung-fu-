package com.kungfuchess.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;

/**
 * Reads the "Board:" / "Commands:" text fixture from a {@link BufferedReader} and
 * produces a {@link ParsedBoard}.
 *
 * <p>This class owns the only knowledge of the input <em>syntax</em> in the whole
 * codebase; {@link Board} itself only knows about rows, columns, and {@link Piece}
 * placement, never about text formatting. A future {@code BinaryBoardParser} (or
 * any other format) could be introduced as a sibling class in this same package
 * that decodes its own input and calls the exact same {@code new Board(rows, cols)} /
 * {@code board.setPiece(...)} primitives used here, returning its own
 * {@link ParsedBoard}. Nothing downstream -- {@code com.kungfuchess.engine.Game},
 * which is the only class {@code com.kungfuchess.ui.Main} hands the resulting
 * {@link Board} to -- would need to change, since {@code Game} never knows or
 * cares how that {@code Board} came to be populated; it only calls the same
 * public {@code Board} API regardless of which parser built it.
 */
public final class BoardParser {

    private BoardParser() {
    }

    public static ParsedBoard parse(BufferedReader reader) throws IOException {
        List<String> rowLines = new ArrayList<>();
        boolean commandsSectionPresent = false;
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!rowLines.isEmpty()) {
                    break;
                }
                continue;
            }
            if (trimmed.equalsIgnoreCase(GameConfig.BOARD_HEADER)) {
                continue;
            }
            if (trimmed.equalsIgnoreCase(GameConfig.COMMANDS_HEADER)) {
                commandsSectionPresent = true;
                break;
            }
            rowLines.add(trimmed);
        }

        if (rowLines.isEmpty()) {
            throw new BoardParseException(ParseErrorCode.EMPTY_BOARD, "No board rows found in input");
        }

        int cols = rowLines.get(0).split("\\s+").length;
        int rows = rowLines.size();
        Board board = new Board(rows, cols);

        for (int r = 0; r < rows; r++) {
            String[] tokens = rowLines.get(r).split("\\s+");
            if (tokens.length != cols) {
                throw new BoardParseException(ParseErrorCode.ROW_WIDTH_MISMATCH,
                        "Row " + r + " has " + tokens.length + " columns, expected " + cols);
            }
            for (int c = 0; c < cols; c++) {
                String token = tokens[c];
                if (!token.equals(GameConfig.EMPTY_CELL_TOKEN)) {
                    board.setPiece(r, c, Piece.fromToken(token));
                }
            }
        }

        return new ParsedBoard(board, commandsSectionPresent);
    }
}
