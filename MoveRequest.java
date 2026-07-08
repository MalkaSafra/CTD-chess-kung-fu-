public final class MoveRequest {

    public static final long MOVE_DURATION_PER_CELL_MS = 1000;
    public static final long JUMP_DURATION_MS = 1000;

    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;
    private final long requestedAtMs;
    private final long completesAtMs;
    private final Color color;

    MoveRequest(int fromRow, int fromCol, int toRow, int toCol, long requestedAtMs, Color color) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.requestedAtMs = requestedAtMs;
        this.color = color;
        if (fromRow == toRow && fromCol == toCol) {
            this.completesAtMs = requestedAtMs + JUMP_DURATION_MS;
        } else {
            int distance = Math.max(Math.abs(toRow - fromRow), Math.abs(toCol - fromCol));
            this.completesAtMs = requestedAtMs + distance * MOVE_DURATION_PER_CELL_MS;
        }
    }

    public boolean isJump() {
        return fromRow == toRow && fromCol == toCol;
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

    public Color getColor() {
        return color;
    }
}
