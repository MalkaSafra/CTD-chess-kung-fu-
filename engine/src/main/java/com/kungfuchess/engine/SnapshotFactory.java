package com.kungfuchess.engine;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.realtime.Motion;
import com.kungfuchess.realtime.RealTimeArbiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds the read-only {@link GameSnapshot} a renderer draws from: resolves every piece's pixel
 * position -- interpolating a mid-flight move's source/destination/progress into a single point --
 * and how long it's been in its current state, so the renderer itself never touches {@link Motion}
 * or does any of this math.
 */
final class SnapshotFactory {

    private final RealTimeArbiter arbiter;

    SnapshotFactory(RealTimeArbiter arbiter) {
        this.arbiter = arbiter;
    }

    GameSnapshot build(Board board, Position selectedPosition, boolean gameOver, PieceColor winner,
                       List<MoveRecord> moveHistory, Set<Position> legalDestinations, List<TransientEvent> events) {
        List<PieceSnapshot> pieces = new ArrayList<>();
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Piece piece = board.getPiece(new Position(row, col));
                if (piece != null) {
                    pieces.add(snapshotOf(piece));
                }
            }
        }
        return new GameSnapshot(board.getRows(), board.getCols(), pieces, selectedPosition, gameOver,
                arbiter.getScore(PieceColor.WHITE), arbiter.getScore(PieceColor.BLACK), winner, moveHistory,
                legalDestinations, events);
    }

    private PieceSnapshot snapshotOf(Piece piece) {
        if (piece.getState() == PieceState.MOVING) {
            return movingSnapshot(piece);
        }

        double pixelX = piece.getPosition().getCol() * GameConfig.CELL_SIZE_PX;
        double pixelY = piece.getPosition().getRow() * GameConfig.CELL_SIZE_PX;

        return new PieceSnapshot(piece.getKind(), piece.getColor(), piece.getState(), piece.getPosition(),
                pixelX, pixelY, stateElapsedMillis(piece));
    }

    private PieceSnapshot movingSnapshot(Piece piece) {
        Motion motion = arbiter.getActiveMotion(piece);
        double progress = Math.min(1.0, motion.getElapsedMs() / (double) motion.getTotalDurationMs());

        double startX = motion.getSource().getCol() * GameConfig.CELL_SIZE_PX;
        double startY = motion.getSource().getRow() * GameConfig.CELL_SIZE_PX;
        double endX = motion.getDestination().getCol() * GameConfig.CELL_SIZE_PX;
        double endY = motion.getDestination().getRow() * GameConfig.CELL_SIZE_PX;

        double pixelX = startX + (endX - startX) * progress;
        double pixelY = startY + (endY - startY) * progress;

        return new PieceSnapshot(piece.getKind(), piece.getColor(), PieceState.MOVING, piece.getPosition(),
                pixelX, pixelY, motion.getElapsedMs());
    }

    private long stateElapsedMillis(Piece piece) {
        switch (piece.getState()) {
            case JUMPING:
                return arbiter.getActiveMotion(piece).getElapsedMs();
            case SHORT_REST:
                return GameConfig.SHORT_REST_DURATION_MS - arbiter.getRestRemainingMs(piece);
            case LONG_REST:
                return GameConfig.LONG_REST_DURATION_MS - arbiter.getRestRemainingMs(piece);
            case IDLE:
                // An idle piece's breathing loop has no moment it "began" -- it can only be
                // phased against the game's own running clock.
                return arbiter.getTotalElapsedMs();
            default:
                throw new IllegalStateException("Unexpected on-board piece state: " + piece.getState());
        }
    }
}
