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

        boolean legalShape = selection.getPiece().isValidMoveShape(selection.getCol(), selection.getRow(), col, row);
        if (!legalShape) {
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
