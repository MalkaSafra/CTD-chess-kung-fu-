package com.kungfuchess.engine;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.Position;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.MoveOutcome;
import com.kungfuchess.rules.MoveRejection;
import com.kungfuchess.rules.RuleEngine;

public class GameEngine {

    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter arbiter;

    public GameEngine(GameState gameState, RuleEngine ruleEngine, RealTimeArbiter arbiter) {
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.arbiter = arbiter;
    }

    public MoveOutcome requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return MoveOutcome.rejected(MoveRejection.GAME_OVER);
        }

        MoveOutcome validation = ruleEngine.validateMove(gameState.getBoard(), source, destination);
        if (!validation.isAccepted()) {
            return validation;
        }

        Piece piece = gameState.getBoard().getPiece(source);

        if (arbiter.isPieceMoving(piece)) {
            return MoveOutcome.rejected(MoveRejection.PIECE_ALREADY_MOVING);
        }

        if (arbiter.isDestinationTargetedByColor(destination, piece.getColor())) {
            return MoveOutcome.rejected(MoveRejection.FRIENDLY_FIRE_AVOIDED);
        }

        arbiter.startMotion(piece, source, destination, gameState.getBoard());
        return MoveOutcome.ACCEPTED;
    }

    public MoveOutcome requestJump(Position position) {
        if (gameState.isGameOver()) {
            return MoveOutcome.rejected(MoveRejection.GAME_OVER);
        }

        Board board = gameState.getBoard();
        if (!board.isInBounds(position)) {
            return MoveOutcome.rejected(MoveRejection.OUTSIDE_BOARD);
        }

        Piece piece = board.getPiece(position);
        if (piece == null) {
            return MoveOutcome.rejected(MoveRejection.EMPTY_SOURCE);
        }

        if (arbiter.isPieceMoving(piece)) {
            return MoveOutcome.rejected(MoveRejection.PIECE_ALREADY_MOVING);
        }

        arbiter.startJump(piece, position);
        return MoveOutcome.ACCEPTED;
    }

    public void waitClock(long ms) {
        boolean kingCaptured = arbiter.advanceTime(ms, gameState.getBoard());
        if (kingCaptured) {
            gameState.setGameOver(true);
        }
    }

    public Board getBoard() {
        return gameState.getBoard();
    }
}