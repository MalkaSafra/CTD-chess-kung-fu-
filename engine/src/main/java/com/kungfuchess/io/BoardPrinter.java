package com.kungfuchess.io;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.Position;

public final class BoardPrinter {

    private BoardPrinter() {
    }

    public static String format(Board board) {
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (col > 0) {
                    builder.append(' ');
                }
                Piece piece = board.getPiece(new Position(row, col));
                builder.append(piece == null ? ProtocolConfig.EMPTY_CELL_TOKEN : token(piece));
            }
            if (row < board.getRows() - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private static String token(Piece piece) {
        return "" + piece.getColor().code() + piece.getKind().code();
    }
}
