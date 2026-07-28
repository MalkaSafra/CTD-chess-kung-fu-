package com.kungfuchess.view;

/**
 * Pixel-layout constants for the rendered game window: the score strips and the coordinate-label
 * margin framing the board. Distinct from {@link com.kungfuchess.config.GameConfig#CELL_SIZE_PX},
 * which stays in the shared config because it's genuinely cross-cutting -- also needed by
 * {@code input}'s pixel-to-cell mapping and by the engine's own pixel-position math, not just by
 * rendering.
 */
public final class RenderConfig {

    private RenderConfig() {
    }

    /** Height of each of the two score strips drawn above and below the board. */
    public static final int SCORE_BAR_HEIGHT_PX = 46;

    /** Width/height of the coordinate-label strip drawn on each of the board's four edges. */
    public static final int COORDINATE_MARGIN_PX = 34;
}
