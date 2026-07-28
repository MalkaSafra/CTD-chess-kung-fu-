package com.kungfuchess.realtime;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.combat.CaptureLedger;
import com.kungfuchess.combat.CombatResolver;
import com.kungfuchess.combat.RestTracker;
import com.kungfuchess.rules.PieceRules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RealTimeArbiter {

    private final List<Motion> activeMotions = new ArrayList<>();
    private final RestTracker restTracker = new RestTracker();
    private final CaptureLedger captureLedger = new CaptureLedger();
    private final CombatResolver combatResolver = new CombatResolver(restTracker, captureLedger);
    private final MovementSpeeds movementSpeeds;
    private long totalElapsedMs;

    public RealTimeArbiter() {
        this(new MovementSpeeds());
    }

    public RealTimeArbiter(MovementSpeeds movementSpeeds) {
        this.movementSpeeds = movementSpeeds;
    }

    public boolean hasActiveMotion() {
        return !activeMotions.isEmpty();
    }

    public boolean isPieceMoving(Piece piece) {
        return piece.getState() == PieceState.MOVING || piece.getState() == PieceState.JUMPING;
    }

    public boolean isPieceResting(Piece piece) {
        return restTracker.isResting(piece);
    }

    public long getRestRemainingMs(Piece piece) {
        return restTracker.getRemainingMs(piece);
    }

    public int getScore(PieceColor color) {
        return captureLedger.getScore(color);
    }

    /** The color whose king has been captured, or {@code null} if the game isn't over. */
    public PieceColor getWinner() {
        return captureLedger.getWinner();
    }

    /**
     * Total real time the game clock has advanced since this arbiter was created. Used to phase
     * an IDLE piece's looping breathing animation, which -- unlike a move, jump, or rest -- has no
     * single moment it "began," so there is nothing else to time it against.
     */
    public long getTotalElapsedMs() {
        return totalElapsedMs;
    }

    /** The in-flight {@link Motion} for {@code piece}, or {@code null} if it isn't moving/jumping. */
    public Motion getActiveMotion(Piece piece) {
        for (Motion motion : activeMotions) {
            if (motion.getPiece() == piece) {
                return motion;
            }
        }
        return null;
    }

    public boolean isDestinationTargetedByColor(Position destination, PieceColor color) {
        for (Motion motion : activeMotions) {
            if (motion.getDestination().equals(destination) && motion.getPiece().getColor() == color) {
                return true;
            }
        }
        return false;
    }

    public void startMotion(Piece piece, Position source, Position destination, Board board) {
        boolean capture = board.getPiece(destination) != null;
        long duration;
        if (capture) {
            duration = GameConfig.CAPTURE_DURATION_MS;
        } else {
            int distance = Math.max(
                    Math.abs(source.getRow() - destination.getRow()),
                    Math.abs(source.getCol() - destination.getCol()));
            duration = distance * movementSpeeds.moveDurationMsPerCell(piece.getKind(), piece.getColor());
        }

        piece.startMoving();
        activeMotions.add(new Motion(piece, source, destination, duration));
    }

    public void startJump(Piece piece, Position position) {
        piece.startJumping();
        long duration = movementSpeeds.jumpDurationMs(piece.getKind(), piece.getColor());
        activeMotions.add(new Motion(piece, position, position, duration));
    }

    public boolean advanceTime(long ms, Board board) {
        totalElapsedMs += ms;
        activeMotions.removeIf(motion -> motion.getPiece().getState() == PieceState.CAPTURED);

        // Ages only rests that were already running before this tick; any rest that starts later
        // in this same call (via resolving an arrival below) begins fresh, unaffected by ms already
        // spent advancing everyone else's clocks this tick.
        restTracker.advance(ms);

        for (Motion motion : activeMotions) {
            motion.addTime(ms);
        }

        // A motion that gets stuck behind a friendly piece it now shares a path with never
        // reaches its original destination, so this must be checked -- and may finalize motions
        // early -- before arrivals (which assume a motion that reaches its total duration lands
        // at its original destination) are resolved.
        applyFriendlyPathBlocking(board);

        // Translations are resolved before jump landings so that an enemy arriving at an
        // airborne piece's cell in this same tick is always checked against it while it's
        // still JUMPING -- regardless of whether the jump's own landing motion happens to
        // sit earlier in the list than the attacker's motion.
        boolean kingCaptured = resolveArrivedTranslations(board);
        resolveArrivedJumps();
        return kingCaptured;
    }

    /**
     * Checks every still-in-flight translation to see whether the elapsed time has now reached a
     * point along its straight-line path that a friendly piece currently occupies. Progress along
     * the path is estimated proportionally against the motion's total duration, since a capture's
     * duration is fixed rather than distance-based and so has no fixed "ms per cell" rate.
     * If blocked, the mover never reaches its original destination: it stops at the last free cell
     * before the block and settles there, regardless of how much travel time it has left.
     */
    private void applyFriendlyPathBlocking(Board board) {
        Iterator<Motion> iterator = activeMotions.iterator();
        while (iterator.hasNext()) {
            Motion motion = iterator.next();
            Piece piece = motion.getPiece();

            if (motion.getSource().equals(motion.getDestination())) {
                continue; // jump: no path to block
            }

            List<Position> path = PieceRules.pathBetween(motion.getSource(), motion.getDestination());
            int totalSteps = path.size();
            for (int i = 0; i < path.size() - 1; i++) {
                // Proportional, not per-cell-constant: a fixed-duration capture motion has no
                // defined "ms per cell," so estimate its progress as a fraction of total travel time.
                long cellArrivalMs = motion.getTotalDurationMs() * (i + 1) / totalSteps;
                if (motion.getElapsedMs() < cellArrivalMs) {
                    break; // hasn't reached this cell yet, so it can't have reached any further one
                }

                Piece occupant = board.getPiece(path.get(i));
                if (occupant != null && occupant.getColor() == piece.getColor()) {
                    Position stopAt = i == 0 ? motion.getSource() : path.get(i - 1);
                    combatResolver.stopShortOfBlock(board, piece, motion.getSource(), stopAt);
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private boolean resolveArrivedTranslations(Board board) {
        List<Motion> arrivedTranslations = new ArrayList<>();

        for (Motion motion : activeMotions) {
            if (motion.getSource().equals(motion.getDestination())) {
                continue; // jump; resolved in resolveArrivedJumps once all arrivals are settled
            }

            if (!motion.isArrived()) {
                continue;
            }

            arrivedTranslations.add(motion);
        }

        // Group by destination first and resolve each destination as one unit, so that two
        // motions landing on the same square in the same tick are never resolved one after
        // the other -- which would let whichever happened to come first in this list
        // silently "win" the square. See CombatResolver.resolveContestedArrival.
        Map<Position, List<CombatResolver.Arrival>> arrivalsByDestination = new LinkedHashMap<>();
        for (Motion motion : arrivedTranslations) {
            long overshootMs = motion.getElapsedMs() - motion.getTotalDurationMs();
            arrivalsByDestination
                    .computeIfAbsent(motion.getDestination(), destination -> new ArrayList<>())
                    .add(new CombatResolver.Arrival(motion.getSource(), motion.getPiece(), overshootMs));
        }

        boolean kingCaptured = false;
        for (Map.Entry<Position, List<CombatResolver.Arrival>> entry : arrivalsByDestination.entrySet()) {
            if (combatResolver.resolveContestedArrival(board, entry.getKey(), entry.getValue())) {
                kingCaptured = true;
            }
        }

        activeMotions.removeAll(arrivedTranslations);
        return kingCaptured;
    }

    private void resolveArrivedJumps() {
        Iterator<Motion> iterator = activeMotions.iterator();
        while (iterator.hasNext()) {
            Motion motion = iterator.next();
            Piece piece = motion.getPiece();

            if (!motion.getSource().equals(motion.getDestination())) {
                continue; // not a jump
            }

            if (!motion.isArrived()) {
                continue;
            }

            // No enemy arrived during the window; the piece settles into a short rest in place.
            piece.enterShortRest();
            restTracker.begin(piece, GameConfig.SHORT_REST_DURATION_MS);
            iterator.remove();
        }
    }
}
