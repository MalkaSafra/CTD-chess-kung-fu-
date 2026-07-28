package com.kungfuchess.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

/**
 * Reads the "Board:" / "Commands:" text fixture from a {@link BufferedReader} and
 * produces a {@link ParsedBoard}.
 *
 * <p>This class owns the only knowledge of the input <em>syntax</em> in the whole
 * codebase; {@link Board} itself only knows about rows, columns, and {@link Piece}
 * placement, never about text formatting.
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
            if (trimmed.equalsIgnoreCase(ProtocolConfig.BOARD_HEADER)) {
                continue;
            }
            if (trimmed.equalsIgnoreCase(ProtocolConfig.COMMANDS_HEADER)) {
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

        for (int row = 0; row < rows; row++) {
            String[] tokens = rowLines.get(row).split("\\s+");
            if (tokens.length != cols) {
                throw new BoardParseException(ParseErrorCode.ROW_WIDTH_MISMATCH,
                        "Row " + row + " has " + tokens.length + " columns, expected " + cols);
            }
            for (int col = 0; col < cols; col++) {
                String token = tokens[col];
                if (!token.equals(ProtocolConfig.EMPTY_CELL_TOKEN)) {
                    if (token.length() != 2) {
                        throw new BoardParseException(ParseErrorCode.UNKNOWN_TOKEN, "Invalid piece token: " + token);
                    }
                    PieceColor color = parseToken(token.charAt(0), PieceColor::fromCode);
                    PieceKind kind = parseToken(token.charAt(1), PieceKind::fromCode);
                    Position position = new Position(row, col);
                    board.addPiece(new Piece(color, kind, position, PieceState.IDLE));
                }
            }
        }

        return new ParsedBoard(board, commandsSectionPresent);
    }

    private static <T> T parseToken(char code, Function<Character, T> fromCode) {
        try {
            return fromCode.apply(code);
        } catch (IllegalArgumentException e) {
            throw new BoardParseException(ParseErrorCode.UNKNOWN_TOKEN, e.getMessage());
        }
    }
}