public final class Rook extends Piece {

    Rook(Color color) {
        super(color, PieceType.ROOK);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        boolean sameX = startX == endX;
        boolean sameY = startY == endY;
        return sameX ^ sameY;
    }

    @Override
    public boolean requiresClearPath() {
        return true;
    }
}
