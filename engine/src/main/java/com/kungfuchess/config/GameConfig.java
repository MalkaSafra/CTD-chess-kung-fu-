package com.kungfuchess.config;

/**
 * Central home for gameplay-balance timing, plus the one pixel constant ({@link #CELL_SIZE_PX})
 * that's genuinely cross-cutting -- needed by {@code input}'s pixel-to-cell mapping and the
 * engine's own pixel-position math, not just by rendering. Layer-specific constants live in that
 * layer's own config class instead: {@link com.kungfuchess.view.RenderConfig} (rendering layout),
 * {@link com.kungfuchess.io.ProtocolConfig} (board-fixture text syntax), and
 * {@link com.kungfuchess.input.CommandSyntax} (command-line keywords). Values that define chess
 * itself (e.g. a King's one-cell range) belong to {@code rules}, not here.
 */
public final class GameConfig {

    private GameConfig() {
    }

    public static final int CELL_SIZE_PX = 100;

    public static final long MOVE_DURATION_PER_CELL_MS = 1000;

    public static final long JUMP_DURATION_MS = 1000;

    /** Fixed, unlike {@link #MOVE_DURATION_PER_CELL_MS} -- a capture never scales with distance. */
    public static final long CAPTURE_DURATION_MS = 1000;

    /** How long a piece is unable to act after landing a jump. */
    public static final long SHORT_REST_DURATION_MS = 500;

    /** How long a piece is unable to act after completing a translation (a plain move or a capture). */
    public static final long LONG_REST_DURATION_MS = 1000;
}
