public final class Rook extends Piece {

    public Rook(Color color) {
        super(color, PieceType.ROOK);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        boolean sameX = startX == endX;
        boolean sameY = startY == endY;
        return sameX ^ sameY;
    }
}
