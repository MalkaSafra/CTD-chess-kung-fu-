package com.kungfuchess.config;

/**
 * Central home for values that describe how this specific game is tuned,
 * as opposed to values that define chess itself (e.g. a King's one-cell
 * range or a Knight's L-shape are rules of the game, not configuration).
 *
 * <p>Keeping these here means retuning the game (e.g. faster moves, a
 * different board-fixture syntax) never requires touching the classes
 * that implement the game's behavior.
 */
public final class GameConfig {

    private GameConfig() {
    }

    public static final int CELL_SIZE_PX = 100;

    /** Height of each of the two score strips drawn above and below the board. */
    public static final int SCORE_BAR_HEIGHT_PX = 40;

    public static final long MOVE_DURATION_PER_CELL_MS = 1000;

    public static final long JUMP_DURATION_MS = 1000;

    /** Fixed, unlike {@link #MOVE_DURATION_PER_CELL_MS} -- a capture never scales with distance. */
    public static final long CAPTURE_DURATION_MS = 1000;

    /** How long a piece is unable to act after landing a jump. */
    public static final long SHORT_REST_DURATION_MS = 500;

    /** How long a piece is unable to act after completing a translation (a plain move or a capture). */
    public static final long LONG_REST_DURATION_MS = 1000;

    public static final String EMPTY_CELL_TOKEN = ".";

    public static final String BOARD_HEADER = "Board:";

    public static final String COMMANDS_HEADER = "Commands:";

    public static final String COMMAND_CLICK = "click";

    public static final String COMMAND_JUMP = "jump";

    public static final String COMMAND_WAIT = "wait";

    public static final String COMMAND_PRINT = "print";

    /** Currently the only supported argument to {@link #COMMAND_PRINT}. */
    public static final String PRINT_ARG_BOARD = "board";

    /** Prefix for the single-line VPL-facing error output, followed by a {@code ParseErrorCode} name. */
    public static final String ERROR_OUTPUT_PREFIX = "ERROR ";

    /** Prefix for the internal, stderr-only I/O failure message (never seen by VPL). */
    public static final String IO_ERROR_PREFIX = "Error: ";
}
