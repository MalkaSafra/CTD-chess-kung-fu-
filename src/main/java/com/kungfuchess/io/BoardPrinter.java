package com.kungfuchess.io;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.Position;

public final class BoardPrinter {

    private BoardPrinter() {
    }

    public static String format(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (c > 0) {
                    sb.append(' ');
                }
                Piece piece = board.getPiece(new Position(r, c));
                sb.append(piece == null ? GameConfig.EMPTY_CELL_TOKEN : token(piece));
            }
            if (r < board.getRows() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String token(Piece piece) {
        return "" + piece.getColor().code() + piece.getKind().code();
    }
}
