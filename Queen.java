public final class Queen extends Piece {

    private final Rook rookShape;
    private final Bishop bishopShape;

    Queen(Color color) {
        super(color, PieceType.QUEEN);
        this.rookShape = new Rook(color);
        this.bishopShape = new Bishop(color);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        return rookShape.isValidMoveShape(startX, startY, endX, endY)
                || bishopShape.isValidMoveShape(startX, startY, endX, endY);
    }

    @Override
    public boolean requiresClearPath() {
        return true;
    }
}
