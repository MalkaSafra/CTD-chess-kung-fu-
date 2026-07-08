import java.util.ArrayList;
import java.util.List;

public final class MoveRequestQueue {

    private final List<MoveRequest> pending = new ArrayList<>();

    public void enqueue(MoveRequest request) {
        pending.add(request);
    }

    public void processUpTo(long currentTimeMs, Board board) {
        pending.removeIf(request -> {
            if (board.isGameOver()) {
                return true;
            }
            if (request.getCompletesAtMs() > currentTimeMs) {
                return false;
            }
            settle(request, board);
            return true;
        });
    }

    private void settle(MoveRequest request, Board board) {
        Piece movingPiece = board.getPiece(request.getFromRow(), request.getFromCol());
        if (movingPiece == null) {
            return;
        }
        Piece targetPiece = board.getPiece(request.getToRow(), request.getToCol());
        if (targetPiece != null && targetPiece.getColor() == movingPiece.getColor()) {
            return;
        }
        board.movePiece(request.getFromRow(), request.getFromCol(), request.getToRow(), request.getToCol());
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
