public final class MoveRequest {

    // Placeholder until per-piece movement speeds are defined; requests settle instantly for now.
    private static final long DEFAULT_DURATION_MS = 0;

    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;
    private final long requestedAtMs;
    private final long completesAtMs;

    public MoveRequest(int fromRow, int fromCol, int toRow, int toCol, long requestedAtMs) {
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
