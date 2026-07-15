package com.kungfuchess.combat;

import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Times how long each resting piece (SHORT_REST/LONG_REST) has left before it can act again.
 * Separate from {@link com.kungfuchess.realtime.Motion}: a resting piece isn't mid-translation or
 * mid-jump, so it has no path or destination to track -- just a countdown back to IDLE.
 */
public final class RestTracker {

    private final Map<Piece, Long> remainingMs = new HashMap<>();

    public void begin(Piece piece, long durationMs) {
        remainingMs.put(piece, durationMs);
    }

    public void advance(long ms) {
        Iterator<Map.Entry<Piece, Long>> iterator = remainingMs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Piece, Long> entry = iterator.next();
            Piece piece = entry.getKey();

            if (piece.getState() == PieceState.CAPTURED) {
                iterator.remove();
                continue;
            }

            long remaining = entry.getValue() - ms;
            if (remaining <= 0) {
                piece.setState(PieceState.IDLE);
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    public boolean isResting(Piece piece) {
        return remainingMs.containsKey(piece);
    }

    public long getRemainingMs(Piece piece) {
        Long remaining = remainingMs.get(piece);
        if (remaining == null) {
            throw new IllegalStateException("Piece is not resting: " + piece);
        }
        return remaining;
    }
}
