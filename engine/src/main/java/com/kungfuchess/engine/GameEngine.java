package com.kungfuchess.engine;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.MoveOutcome;
import com.kungfuchess.rules.MoveRejection;
import com.kungfuchess.rules.PieceRules;
import com.kungfuchess.rules.RuleEngine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameEngine {

    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter arbiter;
    private final SnapshotFactory snapshotFactory;
    private final MoveHistory moveHistory = new MoveHistory();
    private final List<PromotionListener> promotionListeners = new ArrayList<>();
    private final List<TransientEvent> pendingEvents = new ArrayList<>();

    public GameEngine(GameState gameState, RuleEngine ruleEngine, RealTimeArbiter arbiter) {
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.arbiter = arbiter;
        this.snapshotFactory = new SnapshotFactory(arbiter);
    }

    /** Notifies {@code listener} synchronously, on the caller's own thread, whenever a move or jump is recorded. */
    public void addMoveListener(MoveListener listener) {
        moveHistory.addListener(listener);
    }

    /** Notified synchronously whenever a pawn is promoted to a queen. */
    @FunctionalInterface
    public interface PromotionListener {
        void onPromoted(Position position, PieceColor color);
    }

    public void addPromotionListener(PromotionListener listener) {
        promotionListeners.add(listener);
    }

    /**
     * Builds a snapshot for rendering, including every {@link TransientEvent} since the last call
     * to this method -- read-and-clear, so each event is reported exactly once. Every current
     * caller takes exactly one snapshot per {@link #waitClock} tick, which is what makes "since
     * the last call" line up with "this tick" in practice.
     */
    public GameSnapshot snapshot(Position selectedPosition) {
        Board board = gameState.getBoard();
        Set<Position> legalDestinations = legalDestinationsFor(board, selectedPosition);
        List<TransientEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return snapshotFactory.build(board, selectedPosition, gameState.isGameOver(),
                gameState.getWinner(), moveHistory.getAll(), legalDestinations, events);
    }

    /**
     * The squares a player could highlight as "you may move here" for whichever piece is
     * currently selected -- geometrically legal per {@link PieceRules}, minus any square already
     * held by one of the selected piece's own side (mirrors the {@code FRIENDLY_DESTINATION}
     * check {@link RuleEngine#validateMove} applies to an actual move request).
     */
    private Set<Position> legalDestinationsFor(Board board, Position selectedPosition) {
        if (selectedPosition == null) {
            return Set.of();
        }
        Piece piece = board.getPiece(selectedPosition);
        if (piece == null) {
            return Set.of();
        }

        Set<Position> destinations = new HashSet<>(PieceRules.getLegalDestinations(board, piece));
        destinations.removeIf(destination -> {
            Piece occupant = board.getPiece(destination);
            return occupant != null && occupant.getColor() == piece.getColor();
        });
        return destinations;
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

        if (arbiter.isPieceResting(piece)) {
            return MoveOutcome.rejected(MoveRejection.PIECE_RESTING);
        }

        if (arbiter.isDestinationTargetedByColor(destination, piece.getColor())) {
            return MoveOutcome.rejected(MoveRejection.FRIENDLY_FIRE_AVOIDED);
        }

        arbiter.startMotion(piece, source, destination, gameState.getBoard());
        moveHistory.record(piece.getColor(), piece.getKind(), source, destination, arbiter.getTotalElapsedMs());
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

        if (arbiter.isPieceResting(piece)) {
            return MoveOutcome.rejected(MoveRejection.PIECE_RESTING);
        }

        arbiter.startJump(piece, position);
        moveHistory.record(piece.getColor(), piece.getKind(), position, position, arbiter.getTotalElapsedMs());
        return MoveOutcome.ACCEPTED;
    }

    /**
     * Ends the game immediately in {@code resigningColor}'s opponent's favor -- e.g. the server's
     * 20-second disconnect auto-resign. A no-op if the game is already over (idempotent, so a
     * caller racing a natural king-capture end doesn't need to check first).
     */
    public void resign(PieceColor resigningColor) {
        if (gameState.isGameOver()) {
            return;
        }
        gameState.setGameOver(true);
        gameState.setWinner(resigningColor.opposite());
    }

    public void waitClock(long ms) {
        boolean kingCaptured = arbiter.advanceTime(ms, gameState.getBoard());
        promotePawnsOnBackRank();
        if (kingCaptured) {
            gameState.setGameOver(true);
            gameState.setWinner(arbiter.getWinner());
        }
    }

    public Board getBoard() {
        return gameState.getBoard();
    }

    /**
     * A pawn that has just arrived on the back rank -- the far edge from the side it started
     * on -- becomes a queen. Checked once per tick, after all of this tick's arrivals have
     * settled, rather than at the moment of any single arrival: promotion is a rule about what a
     * piece becomes, which is the engine's call to make, not something combat resolution (which
     * only decides who occupies a contested square) should be reaching into pieces to do.
     */
    private void promotePawnsOnBackRank() {
        Board board = gameState.getBoard();
        for (int col = 0; col < board.getCols(); col++) {
            promoteIfEligible(board, new Position(0, col), PieceColor.WHITE);
            promoteIfEligible(board, new Position(board.getRows() - 1, col), PieceColor.BLACK);
        }
    }

    private void promoteIfEligible(Board board, Position position, PieceColor promotingColor) {
        Piece piece = board.getPiece(position);
        if (piece != null && piece.getKind() == PieceKind.PAWN && piece.getColor() == promotingColor) {
            piece.promoteToQueen();
            pendingEvents.add(TransientEvent.PAWN_PROMOTED);
            for (PromotionListener listener : promotionListeners) {
                listener.onPromoted(position, promotingColor);
            }
        }
    }
}