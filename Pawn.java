public final class Pawn extends Piece {

    Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public boolean isValidMoveShape(int startX, int startY, int endX, int endY) {
        // Pawn movement rules are out of scope for this iteration; no move is legal yet.
        return false;
    }
}
