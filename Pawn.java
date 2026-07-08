public final class Pawn extends Piece {

    Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        int direction = getColor() == Color.WHITE ? -1 : 1;
        int rowDelta = endY - startY;
        int colDelta = Math.abs(endX - startX);
        boolean singleStep = rowDelta == direction && colDelta <= 1;
        boolean doubleStep = rowDelta == 2 * direction && colDelta == 0;
        return singleStep || doubleStep;
    }

    @Override
    public boolean isValidMove(Board board, int startRow, int startCol, int endRow, int endCol) {
        if (!isValidMoveShape(startCol, startRow, endCol, endRow)) {
            return false;
        }

        int direction = getColor() == Color.WHITE ? -1 : 1;
        int rowDelta = endRow - startRow;
        Piece destinationPiece = board.getPiece(endRow, endCol);

        if (rowDelta == 2 * direction) {
            int initialRow = getColor() == Color.WHITE ? board.getRows() - 2 : 1;
            return startRow == initialRow
                    && board.isPathClear(startRow, startCol, endRow, endCol)
                    && destinationPiece == null;
        }

        boolean movingStraight = endCol == startCol;
        if (movingStraight) {
            return destinationPiece == null;
        }

        return destinationPiece != null && destinationPiece.getColor() != getColor();
    }
}
