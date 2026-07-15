package com.kungfuchess.combat;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.rules.PieceRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves what happens when one or more pieces' motions arrive at the same destination in the
 * same tick, or when a piece's path is blocked by a friendly piece it now shares a square with:
 * airborne interception, friendly blocking, and enemy captures.
 *
 * <p>Two collision rules govern every contested square:
 * <ul>
 *     <li><b>Enemy collisions:</b> whichever piece arrives <em>later</em> captures whichever
 *     arrived earlier (a piece already standing on the square always counts as "earlier" than
 *     any piece arriving this tick). See {@link #resolveContestedArrival}.</li>
 *     <li><b>Friendly collisions:</b> a piece can never share a square, or pass over a square
 *     occupied by, a friendly piece. Whichever piece arrives later simply stops one square short
 *     of the collision point instead of landing on it. See {@link #stopShortOfBlock} for the
 *     mid-path case and the friendly branches below for the destination-square case.</li>
 * </ul>
 */
public final class CombatResolver {

    private final RestTracker restTracker;
    private final CaptureLedger captureLedger;

    public CombatResolver(RestTracker restTracker, CaptureLedger captureLedger) {
        this.restTracker = restTracker;
        this.captureLedger = captureLedger;
    }

    /** One piece's pending arrival at a destination: where it came from, who it is, and how much
     * time elapsed past its own finish line this tick (smaller means it arrived more recently). */
    public static final class Arrival {
        private final Position source;
        private final Piece piece;
        private final long overshootMs;

        public Arrival(Position source, Piece piece, long overshootMs) {
            this.source = source;
            this.piece = piece;
            this.overshootMs = overshootMs;
        }

        public Position getSource() {
            return source;
        }

        public Piece getPiece() {
            return piece;
        }

        public long getOvershootMs() {
            return overshootMs;
        }
    }

    public boolean resolveArrival(Board board, Position source, Position destination, Piece movingPiece) {
        return resolveContestedArrival(board, destination, Collections.singletonList(new Arrival(source, movingPiece, 0)));
    }

    /**
     * Resolves every motion that arrived at {@code destination} in the same tick as a single
     * unit, so the outcome depends only on the pieces and the board -- never on the order the
     * caller happens to list them in.
     */
    public boolean resolveContestedArrival(Board board, Position destination, List<Arrival> arrivals) {
        List<Arrival> pending = withoutAlreadyCaptured(arrivals);
        if (pending.isEmpty()) {
            // Every mover heading here was already captured while resolving a different
            // destination earlier in this same tick -- e.g. two enemies simultaneously trading
            // captures by swapping squares. There is no one left to complete this arrival.
            return false;
        }

        Piece defender = board.getPiece(destination);

        if (defender != null && defender.getState() == PieceState.JUMPING) {
            return resolveAgainstAirborneDefender(board, pending, defender);
        }

        if (pending.size() == 1) {
            return completeSingleArrival(board, pending.get(0), destination, defender);
        }

        return resolveLaterEatsEarlier(board, destination, pending, defender);
    }

    private List<Arrival> withoutAlreadyCaptured(List<Arrival> arrivals) {
        List<Arrival> pending = new ArrayList<>();
        for (Arrival arrival : arrivals) {
            if (arrival.getPiece().getState() != PieceState.CAPTURED) {
                pending.add(arrival);
            }
        }
        return pending;
    }

    /**
     * A piece whose path has just crossed into a cell a friendly piece currently occupies never
     * reaches its original destination: it settles at {@code stopAt} (the last free cell before
     * the block, or its own source if the very first step is blocked) instead.
     */
    public void stopShortOfBlock(Board board, Piece piece, Position source, Position stopAt) {
        if (!stopAt.equals(source)) {
            board.removePiece(source);
            piece.setPosition(stopAt);
            board.addPiece(piece);
        }
        enterLongRest(piece);
    }

    private boolean resolveAgainstAirborneDefender(Board board, List<Arrival> arrivals, Piece defender) {
        boolean kingCaptured = false;
        for (Arrival arrival : arrivals) {
            Piece mover = arrival.getPiece();
            if (mover.getColor() == defender.getColor()) {
                // Landing on one's own airborne piece: never double-occupy, so the mover stops
                // one square short instead of sharing the square with its ally.
                stopOneSquareShort(board, mover, arrival.getSource(), defender.getPosition());
                continue;
            }
            // The destination is held by an airborne enemy: it captures the arriver instead
            // of being captured, and stays airborne (does not move) for the rest of its jump.
            // The arriver never occupied the destination, so it must be cleared from its own
            // source square rather than from the destination.
            kingCaptured |= captureAt(board, mover, arrival.getSource());
        }
        return kingCaptured;
    }

    private boolean completeSingleArrival(Board board, Arrival arrival, Position destination, Piece defender) {
        Piece mover = arrival.getPiece();

        if (defender != null && defender.getColor() == mover.getColor()) {
            // Destination got claimed by a friendly piece while this one was in flight; the
            // mover stops one square short instead of landing on its ally.
            stopOneSquareShort(board, mover, arrival.getSource(), destination);
            return false;
        }

        return completeArrival(board, arrival.getSource(), destination, mover, defender);
    }

    private boolean resolveLaterEatsEarlier(Board board, Position destination, List<Arrival> arrivals, Piece defender) {
        Arrival latest = null;
        long smallestOvershoot = Long.MAX_VALUE;
        boolean tie = false;
        for (Arrival arrival : arrivals) {
            if (arrival.getOvershootMs() < smallestOvershoot) {
                smallestOvershoot = arrival.getOvershootMs();
                latest = arrival;
                tie = false;
            } else if (arrival.getOvershootMs() == smallestOvershoot) {
                tie = true;
            }
        }

        if (tie) {
            // A genuinely indistinguishable arrival instant: there is no defined "later" piece,
            // so every contender -- including any stationary occupant -- destroys the others.
            return resolveMutualDestruction(board, destination, arrivals, defender);
        }

        boolean kingCaptured = false;
        if (defender != null) {
            kingCaptured |= captureAt(board, defender, destination);
        }

        for (Arrival arrival : arrivals) {
            if (arrival == latest) {
                continue;
            }
            kingCaptured |= captureAt(board, arrival.getPiece(), arrival.getSource());
        }

        kingCaptured |= completeArrival(board, latest.getSource(), destination, latest.getPiece(), null);
        return kingCaptured;
    }

    private boolean resolveMutualDestruction(Board board, Position destination,
                                              List<Arrival> arrivals, Piece defender) {
        boolean kingCaptured = false;

        if (defender != null) {
            kingCaptured |= captureAt(board, defender, destination);
        }

        for (Arrival arrival : arrivals) {
            kingCaptured |= captureAt(board, arrival.getPiece(), arrival.getSource());
        }

        return kingCaptured;
    }

    private boolean completeArrival(Board board, Position source, Position destination,
                                     Piece mover, Piece defender) {
        board.removePiece(source);

        boolean kingCaptured = false;
        if (defender != null) {
            kingCaptured = captureAt(board, defender, destination);
        }

        mover.setPosition(destination);
        board.addPiece(mover);
        enterLongRest(mover);

        return kingCaptured;
    }

    private void enterLongRest(Piece piece) {
        piece.setState(PieceState.LONG_REST);
        restTracker.begin(piece, GameConfig.LONG_REST_DURATION_MS);
    }

    private void stopOneSquareShort(Board board, Piece mover, Position source, Position collisionPoint) {
        List<Position> path = PieceRules.pathBetween(source, collisionPoint);
        Position stopAt = path.size() <= 1 ? source : path.get(path.size() - 2);
        stopShortOfBlock(board, mover, source, stopAt);
    }

    private boolean captureAt(Board board, Piece piece, Position position) {
        board.removePiece(position);
        piece.setState(PieceState.CAPTURED);
        captureLedger.recordCapture(piece);
        return piece.getKind() == PieceKind.KING;
    }
}
