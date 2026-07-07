public final class Piece {

    private final Color color;
    private final PieceType type;

    public Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public static Piece fromToken(String token) {
        if (token.length() != 2) {
            throw new BoardParseException(ParseErrorCode.UNKNOWN_TOKEN, "Invalid piece token: " + token);
        }
        try {
            Color color = Color.fromCode(token.charAt(0));
            PieceType type = PieceType.fromCode(token.charAt(1));
            return new Piece(color, type);
        } catch (IllegalArgumentException e) {
            throw new BoardParseException(ParseErrorCode.UNKNOWN_TOKEN, "Invalid piece token: " + token);
        }
    }

    @Override
    public String toString() {
        return "" + color.getCode() + type.getCode();
    }
}
