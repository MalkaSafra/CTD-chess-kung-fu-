public final class King extends Piece {

    King(Color color) {
        super(color, PieceType.KING);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        int dx = Math.abs(endX - startX);
        int dy = Math.abs(endY - startY);
        return (dx != 0 || dy != 0) && dx <= 1 && dy <= 1;
    }
}
