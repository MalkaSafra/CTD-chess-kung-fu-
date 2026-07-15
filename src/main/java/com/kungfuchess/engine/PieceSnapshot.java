package com.kungfuchess.engine;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;

/**
 * A read-only projection of one piece for rendering: pixel position is already resolved (including
 * mid-flight interpolation for a moving piece), so a renderer never needs its own source/destination/
 * progress math -- see {@link SnapshotFactory}.
 */
public record PieceSnapshot(
        PieceKind kind,
        PieceColor color,
        PieceState state,
        double pixelX,
        double pixelY,
        long stateElapsedMillis
) {
}
