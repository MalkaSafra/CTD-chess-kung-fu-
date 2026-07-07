public final class Knight extends Piece {

    Knight(Color color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        int dx = Math.abs(endX - startX);
        int dy = Math.abs(endY - startY);
        return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
    }
}
