import java.io.PrintStream;
import java.util.Optional;

public final class CommandProcessor {

    private final Board board;
    private final PrintStream out;
    private final SelectionManager selectionManager = new SelectionManager();
    private final MoveRequestQueue moveQueue = new MoveRequestQueue();
    private final GameClock clock = new GameClock();

    public CommandProcessor(Board board, PrintStream out) {
        this.board = board;
        this.out = out;
    }

    public void process(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String[] parts = trimmed.split("\\s+");
        switch (parts[0].toLowerCase()) {
            case "click":
                handleClick(parts);
                break;
            case "jump":
                handleJump(parts);
                break;
            case "wait":
                handleWait(parts);
                break;
            case "print":
                handlePrint(parts);
                break;
            default:
                break;
        }
    }

    private void handleClick(String[] parts) {
        int[] cell = parseCell(parts);
        if (cell == null) {
            return;
        }

        moveQueue.processUpTo(clock.getElapsedMs(), board);
        Optional<MoveRequest> request = selectionManager.handleClick(board, moveQueue, cell[0], cell[1],
                clock.getElapsedMs());
        request.ifPresent(moveQueue::enqueue);
        moveQueue.processUpTo(clock.getElapsedMs(), board);
    }

    private void handleJump(String[] parts) {
        int[] cell = parseCell(parts);
        if (cell == null) {
            return;
        }

        moveQueue.processUpTo(clock.getElapsedMs(), board);
        Optional<MoveRequest> request = selectionManager.handleJumpCommand(board, moveQueue, cell[0], cell[1],
                clock.getElapsedMs());
        request.ifPresent(moveQueue::enqueue);
        moveQueue.processUpTo(clock.getElapsedMs(), board);
    }

    private int[] parseCell(String[] parts) {
        if (parts.length < 3) {
            return null;
        }
        int x;
        int y;
        try {
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        int row = CoordinateConverter.toRow(y);
        int col = CoordinateConverter.toCol(x);
        if (!board.isInBounds(row, col)) {
            return null;
        }
        return new int[] { row, col };
    }

    private void handleWait(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        long ms;
        try {
            ms = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        clock.advance(ms);
        moveQueue.processUpTo(clock.getElapsedMs(), board);
    }

    private void handlePrint(String[] parts) {
        if (parts.length < 2 || !parts[1].equalsIgnoreCase("board")) {
            return;
        }
        moveQueue.processUpTo(clock.getElapsedMs(), board);
        out.println(board.toCanonicalString());
    }
}
