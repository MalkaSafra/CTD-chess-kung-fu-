import java.util.Optional;

public final class SelectionManager {

    private Selection selection;

    public Optional<MoveRequest> handleClick(Board board, int row, int col, long currentTimeMs) {
        Piece clickedPiece = board.getPiece(row, col);

        if (selection == null) {
            if (clickedPiece == null) {
                return Optional.empty();
            }
            selection = new Selection(row, col, clickedPiece);
            return Optional.empty();
        }

        if (clickedPiece != null && clickedPiece.getColor() == selection.getPiece().getColor()) {
            selection = new Selection(row, col, clickedPiece);
            return Optional.empty();
        }

        if (!isLegalMove(board, selection, row, col)) {
            return Optional.empty();
        }

        MoveRequest request = new MoveRequest(selection.getRow(), selection.getCol(), row, col, currentTimeMs);
        selection = null;
        return Optional.of(request);
    }

    private boolean isLegalMove(Board board, Selection selection, int row, int col) {
        Piece piece = selection.getPiece();

        boolean legalShape = piece.isValidMoveShape(selection.getCol(), selection.getRow(), col, row);
        if (!legalShape) {
            return false;
        }

        if (piece.requiresClearPath() && !board.isPathClear(selection.getRow(), selection.getCol(), row, col)) {
            return false;
        }

        Piece destinationPiece = board.getPiece(row, col);
        if (destinationPiece != null && destinationPiece.getColor() == piece.getColor()) {
            return false;
        }

        return true;
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
