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

    /** Pixel width/height of a single board cell, used to convert clicks to (row, col). */
    public static final int CELL_SIZE_PX = 100;

    /** Milliseconds a sliding/repositioning move takes per cell of distance traveled. */
    public static final long MOVE_DURATION_PER_CELL_MS = 1000;

    /** Fixed milliseconds a jump keeps a piece airborne on its own cell. */
    public static final long JUMP_DURATION_MS = 1000;

    /** Fixed milliseconds any capture takes to resolve, regardless of travel distance. */
    public static final long CAPTURE_DURATION_MS = 1000;

    /** Token representing an empty board cell in both input and canonical output. */
    public static final String EMPTY_CELL_TOKEN = ".";

    /** Fixture line introducing the board grid rows. */
    public static final String BOARD_HEADER = "Board:";

    /** Fixture line marking the end of the board grid and the start of commands. */
    public static final String COMMANDS_HEADER = "Commands:";

    /** Command keyword: select or target a cell by pixel coordinates. */
    public static final String COMMAND_CLICK = "click";

    /** Command keyword: jump the piece at a cell by pixel coordinates. */
    public static final String COMMAND_JUMP = "jump";

    /** Command keyword: advance the game clock by a millisecond amount. */
    public static final String COMMAND_WAIT = "wait";

    /** Command keyword: print output, currently only "print board" is supported. */
    public static final String COMMAND_PRINT = "print";

    /** Argument required after {@link #COMMAND_PRINT} to print the canonical board. */
    public static final String PRINT_ARG_BOARD = "board";

    /** Prefix for the single-line VPL-facing error output, followed by a {@code ParseErrorCode} name. */
    public static final String ERROR_OUTPUT_PREFIX = "ERROR ";

    /** Prefix for the internal, stderr-only I/O failure message (never seen by VPL). */
    public static final String IO_ERROR_PREFIX = "Error: ";
}
