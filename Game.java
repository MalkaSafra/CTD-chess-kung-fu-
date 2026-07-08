import java.io.PrintStream;
import java.util.Optional;

/**
 * Facade over a single game's live state and lifecycle.
 *
 * <p>Owns the {@link Board}, {@link GameClock}, {@link MoveRequestQueue}, and
 * {@link SelectionManager}, and is the only class that knows how those four
 * collaborators interact. Callers such as {@link CommandProcessor} never see
 * a row, a column, or a queue -- only pixel coordinates and millisecond
 * durations in, and a printed board out. That keeps command parsing and game
 * orchestration as two separate responsibilities in two separate classes.
 */
public final class Game {

    private final Board board;
    private final PrintStream out;
    private final GameClock clock = new GameClock();
    private final MoveRequestQueue moveQueue = new MoveRequestQueue();
    private final SelectionManager selectionManager = new SelectionManager();

    public Game(Board board, PrintStream out) {
        this.board = board;
        this.out = out;
    }

    public void handleClick(int x, int y) {
        int row = CoordinateConverter.toRow(y);
        int col = CoordinateConverter.toCol(x);
        if (!board.isInBounds(row, col)) {
            return;
        }

        moveQueue.processUpTo(clock.getElapsedMs(), board);
        Optional<MoveRequest> request = selectionManager.handleClick(board, moveQueue, row, col,
                clock.getElapsedMs());
        request.ifPresent(moveQueue::enqueue);
        moveQueue.processUpTo(clock.getElapsedMs(), board);
    }

    public void handleJump(int x, int y) {
        int row = CoordinateConverter.toRow(y);
        int col = CoordinateConverter.toCol(x);
        if (!board.isInBounds(row, col)) {
            return;
        }

        moveQueue.processUpTo(clock.getElapsedMs(), board);
        Optional<MoveRequest> request = selectionManager.handleJumpCommand(board, moveQueue, row, col,
                clock.getElapsedMs());
        request.ifPresent(moveQueue::enqueue);
        moveQueue.processUpTo(clock.getElapsedMs(), board);
    }

    public void waitClock(long ms) {
        clock.advance(ms);
        moveQueue.processUpTo(clock.getElapsedMs(), board);
    }

    public void printBoard() {
        moveQueue.processUpTo(clock.getElapsedMs(), board);
        out.println(board.toCanonicalString());
    }
}

final class CoordinateConverter {

    private CoordinateConverter() {
    }

    static int toRow(int y) {
        return Math.floorDiv(y, GameConfig.CELL_SIZE_PX);
    }

    static int toCol(int x) {
        return Math.floorDiv(x, GameConfig.CELL_SIZE_PX);
    }
}
