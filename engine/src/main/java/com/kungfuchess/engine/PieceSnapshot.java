package com.kungfuchess.engine;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

/**
 * A read-only projection of one piece for rendering: pixel position is already resolved (including
 * mid-flight interpolation for a moving piece), so a renderer never needs its own source/destination/
 * progress math -- see {@link SnapshotFactory}. {@code position} is the piece's logical board cell
 * (stable even mid-flight -- a networked client needs this to reconstruct a {@code Board} locally
 * for legal-move highlighting; a local renderer only ever uses pixelX/pixelY and ignores it).
 */
public record PieceSnapshot(
        PieceKind kind,
        PieceColor color,
        PieceState state,
        Position position,
        double pixelX,
        double pixelY,
        long stateElapsedMillis
) {
}
