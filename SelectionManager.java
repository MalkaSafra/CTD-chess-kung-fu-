import java.util.Optional;

public final class SelectionManager {

    private Selection selection;

    public Optional<MoveRequest> handleClick(Board board, MoveRequestQueue moveQueue, int row, int col,
            long currentTimeMs) {
        Piece clickedPiece = board.getPiece(row, col);
        boolean clickedPieceLocked = clickedPiece != null && moveQueue.isLocked(row, col, currentTimeMs);

        if (selection == null) {
            if (clickedPiece == null || clickedPieceLocked) {
                return Optional.empty();
            }
            selection = new Selection(row, col, clickedPiece);
            return Optional.empty();
        }

        if (clickedPiece != null && clickedPiece.getColor() == selection.getPiece().getColor()) {
            if (clickedPieceLocked) {
                return Optional.empty();
            }
            selection = new Selection(row, col, clickedPiece);
            return Optional.empty();
        }

        boolean legalMove = selection.getPiece().isValidMove(board, selection.getRow(), selection.getCol(), row, col);
        if (!legalMove) {
            return Optional.empty();
        }

        MoveRequest request = new MoveRequest(selection.getRow(), selection.getCol(), row, col, currentTimeMs);
        selection = null;
        return Optional.of(request);
    }
}

final class CoordinateConverter {

    static final int CELL_SIZE_PX = 100;

    private CoordinateConverter() {
    }

    static int toRow(int y) {
        return Math.floorDiv(y, CELL_SIZE_PX);
    }

    static int toCol(int x) {
        return Math.floorDiv(x, CELL_SIZE_PX);
    }
}
