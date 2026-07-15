package com.kungfuchess.engine;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.Position;

import java.util.List;
import java.util.Set;

/**
 * Everything a renderer needs to draw one frame, and nothing else: no live {@code Board} or
 * {@code Piece} reference, so a renderer holding a snapshot has no way to mutate game state.
 */
public record GameSnapshot(
        int boardRows,
        int boardCols,
        List<PieceSnapshot> pieces,
        Position selectedPosition,
        boolean gameOver,
        int whiteScore,
        int blackScore,
        PieceColor winner,
        List<MoveRecord> moveHistory,
        Set<Position> legalDestinations
) {
    public GameSnapshot {
        pieces = List.copyOf(pieces);
        moveHistory = List.copyOf(moveHistory);
        legalDestinations = Set.copyOf(legalDestinations);
    }
}
