public abstract class Piece {

    private final Color color;
    private final PieceType type;

    protected Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public abstract boolean isValidMoveShape(int startX, int startY, int endX, int endY);

    public boolean requiresClearPath() {
        return false;
    }

    public boolean isValidMove(Board board, int startRow, int startCol, int endRow, int endCol) {
        if (!isValidMoveShape(startCol, startRow, endCol, endRow)) {
            return false;
        }
        if (requiresClearPath() && !board.isPathClear(startRow, startCol, endRow, endCol)) {
            return false;
        }
        Piece destinationPiece = board.getPiece(endRow, endCol);
        return destinationPiece == null || destinationPiece.getColor() != getColor();
    }

    public static Piece fromToken(String token) {
        if (token.length() != 2) {
            throw new BoardParseException(ParseErrorCode.UNKNOWN_TOKEN, "Invalid piece token: " + token);
        }
        try {
            Color color = Color.fromCode(token.charAt(0));
            PieceType type = PieceType.fromCode(token.charAt(1));
            return create(color, type);
        } catch (IllegalArgumentException e) {
            throw new BoardParseException(ParseErrorCode.UNKNOWN_TOKEN, "Invalid piece token: " + token);
        }
    }

    private static Piece create(Color color, PieceType type) {
        switch (type) {
            case KING:
                return new King(color);
            case QUEEN:
                return new Queen(color);
            case ROOK:
                return new Rook(color);
            case BISHOP:
                return new Bishop(color);
            case KNIGHT:
                return new Knight(color);
            case PAWN:
                return new Pawn(color);
            default:
                throw new BoardParseException(ParseErrorCode.UNKNOWN_TOKEN, "Unsupported piece type: " + type);
        }
    }

    @Override
    public String toString() {
        return "" + color.getCode() + type.getCode();
    }
}

enum Color {
    WHITE('w'),
    BLACK('b');

    private final char code;

    Color(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static Color fromCode(char code) {
        for (Color color : values()) {
            if (color.code == code) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color code: " + code);
    }
}

enum PieceType {
    KING('K'),
    QUEEN('Q'),
    ROOK('R'),
    BISHOP('B'),
    KNIGHT('N'),
    PAWN('P');

    private final char code;

    PieceType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static PieceType fromCode(char code) {
        for (PieceType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown piece type code: " + code);
    }
}
