public final class MoveRequest {

    public static final long MOVE_DURATION_MS = 2000;

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
        this.completesAtMs = requestedAtMs + MOVE_DURATION_MS;
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
