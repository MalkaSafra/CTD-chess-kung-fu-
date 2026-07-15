package com.kungfuchess.engine;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;

/**
 * One completed move-history entry: who moved what, from where to where, and when (game time in
 * milliseconds since the engine started). A jump is represented the same way as a move, with
 * {@code source} equal to {@code destination} -- the same convention {@code Motion} already uses.
 */
public record MoveRecord(
        PieceColor color,
        PieceKind kind,
        Position source,
        Position destination,
        long timestampMs
) {
    public boolean isJump() {
        return source.equals(destination);
    }
}
