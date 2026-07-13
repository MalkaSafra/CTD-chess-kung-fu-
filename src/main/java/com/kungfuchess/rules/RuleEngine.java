package com.kungfuchess.rules;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.Position;

import java.util.Set;

public class RuleEngine {

    public MoveOutcome validateMove(Board board, Position source, Position destination) {
        if (!board.isInBounds(source) || !board.isInBounds(destination)) {
            return MoveOutcome.rejected(MoveRejection.OUTSIDE_BOARD);
        }

        Piece sourcePiece = board.getPiece(source);
        if (sourcePiece == null) {
            return MoveOutcome.rejected(MoveRejection.EMPTY_SOURCE);
        }

        Piece destinationPiece = board.getPiece(destination);
        if (destinationPiece != null && destinationPiece.getColor() == sourcePiece.getColor()) {
            return MoveOutcome.rejected(MoveRejection.FRIENDLY_DESTINATION);
        }

        Set<Position> legalDestinations = PieceRules.getLegalDestinations(board, sourcePiece);
        if (!legalDestinations.contains(destination)) {
            return MoveOutcome.rejected(MoveRejection.ILLEGAL_PIECE_MOVE);
        }

        return MoveOutcome.ACCEPTED;
    }
}
