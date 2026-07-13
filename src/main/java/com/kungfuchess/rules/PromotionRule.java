package com.kungfuchess.rules;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;

/**
 * Whether a piece transforms as a result of landing on a given square -- currently, a pawn
 * reaching the back rank becomes a queen. This is move-rule knowledge (what a piece becomes),
 * kept separate from combat resolution (who occupies a contested square).
 */
public final class PromotionRule {

    private PromotionRule() {
    }

    public static void apply(Board board, Piece piece, Position destination) {
        if (piece.getKind() != PieceKind.PAWN) {
            return;
        }
        int promotionRow = piece.getColor() == PieceColor.WHITE ? 0 : board.getRows() - 1;
        if (destination.getRow() == promotionRow) {
            piece.setKind(PieceKind.QUEEN);
        }
    }
}
