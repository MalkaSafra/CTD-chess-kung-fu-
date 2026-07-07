public final class Pawn extends Piece {

    Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        int direction = getColor() == Color.WHITE ? -1 : 1;
        int rowDelta = endY - startY;
        int colDelta = Math.abs(endX - startX);
        return rowDelta == direction && colDelta <= 1;
    }

    @Override
    public boolean isValidMove(Board board, int startRow, int startCol, int endRow, int endCol) {
        if (!isValidMoveShape(startCol, startRow, endCol, endRow)) {
            return false;
        }

        Piece destinationPiece = board.getPiece(endRow, endCol);
        boolean movingStraight = endCol == startCol;

        if (movingStraight) {
            return destinationPiece == null;
        }

        return destinationPiece != null && destinationPiece.getColor() != getColor();
    }
}
