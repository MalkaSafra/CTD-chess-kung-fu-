import java.util.Optional;

public final class SelectionManager {

    private Selection selection;

    public Optional<MoveRequest> handleClick(Board board, MoveRequestQueue moveQueue, int row, int col,
            long currentTimeMs) {
        if (board.isGameOver()) {
            return Optional.empty();
        }

        Piece clickedPiece = board.getPiece(row, col);
        boolean clickedPieceLocked = clickedPiece != null && moveQueue.isLocked(row, col, currentTimeMs);

        if (selection == null) {
            if (clickedPiece == null || clickedPieceLocked) {
                return Optional.empty();
            }
            selection = new Selection(row, col, clickedPiece);
            return Optional.empty();
        }

        if (row == selection.getRow() && col == selection.getCol()) {
            MoveRequest jumpRequest = new MoveRequest(row, col, row, col, currentTimeMs,
                    selection.getPiece().getColor(), false);
            selection = null;
            return Optional.of(jumpRequest);
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

        Color movingColor = selection.getPiece().getColor();
        Color oppositeColor = movingColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        if (moveQueue.hasPendingMoveOfColor(oppositeColor, currentTimeMs)) {
            return Optional.empty();
        }

        boolean isCapture = clickedPiece != null;
        MoveRequest request = new MoveRequest(selection.getRow(), selection.getCol(), row, col, currentTimeMs,
                movingColor, isCapture);
        selection = null;
        return Optional.of(request);
    }

    public Optional<MoveRequest> handleJumpCommand(Board board, MoveRequestQueue moveQueue, int row, int col,
            long currentTimeMs) {
        if (board.isGameOver()) {
            return Optional.empty();
        }

        Piece piece = board.getPiece(row, col);
        if (piece == null || moveQueue.isLocked(row, col, currentTimeMs)) {
            return Optional.empty();
        }

        selection = null;
        return Optional.of(new MoveRequest(row, col, row, col, currentTimeMs, piece.getColor(), false));
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
