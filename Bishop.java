public final class Bishop extends Piece {

    Bishop(Color color) {
        super(color, PieceType.BISHOP);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        int dx = Math.abs(endX - startX);
        int dy = Math.abs(endY - startY);
        return dx != 0 && dx == dy;
    }

    @Override
    public boolean requiresClearPath() {
        return true;
    }
}
