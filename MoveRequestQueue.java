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
