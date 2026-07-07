import java.util.ArrayList;
import java.util.List;

public final class MoveRequestQueue {

    private final List<MoveRequest> pending = new ArrayList<>();

    public void enqueue(MoveRequest request) {
        pending.add(request);
    }

    public void processUpTo(long currentTimeMs, Board board) {
        pending.removeIf(request -> {
            if (request.getCompletesAtMs() > currentTimeMs) {
                return false;
            }
            board.movePiece(request.getFromRow(), request.getFromCol(), request.getToRow(), request.getToCol());
            return true;
        });
    }
}

final class MoveRequest {

    // Placeholder until per-piece movement speeds are defined; requests settle instantly for now.
    private static final long DEFAULT_DURATION_MS = 0;

    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;
    private final long requestedAtMs;
    private final long completesAtMs;

    MoveRequest(int fromRow, int fromCol, int toRow, int toCol, long requestedAtMs) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.requestedAtMs = requestedAtMs;
        this.completesAtMs = requestedAtMs + DEFAULT_DURATION_MS;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getFromCol() {
        return fromCol;
    }

    public int getToRow() {
        return toRow;
    }

    public int getToCol() {
        return toCol;
    }

    public long getRequestedAtMs() {
        return requestedAtMs;
    }

    public long getCompletesAtMs() {
        return completesAtMs;
    }
}

final class GameClock {

    private long elapsedMs = 0;

    void advance(long ms) {
        elapsedMs += ms;
    }

    long getElapsedMs() {
        return elapsedMs;
    }
}
