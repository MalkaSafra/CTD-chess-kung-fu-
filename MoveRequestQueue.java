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

    public boolean isLocked(int row, int col, long currentTimeMs) {
        for (MoveRequest request : pending) {
            if (request.getFromRow() == row && request.getFromCol() == col
                    && currentTimeMs < request.getCompletesAtMs()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPendingMoveOfColor(Color color, long currentTimeMs) {
        for (MoveRequest request : pending) {
            if (request.getColor() == color && currentTimeMs < request.getCompletesAtMs()) {
                return true;
            }
        }
        return false;
    }
}
