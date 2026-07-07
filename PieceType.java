public enum PieceType {
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
