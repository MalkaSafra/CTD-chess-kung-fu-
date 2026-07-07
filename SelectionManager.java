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

        MoveRequest request = new MoveRequest(selection.getRow(), selection.getCol(), row, col, currentTimeMs);
        selection = null;
        return Optional.of(request);
    }
}
