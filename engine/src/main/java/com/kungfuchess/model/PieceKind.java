package com.kungfuchess.model;

public enum PieceKind {
    ROOK('R', 5),
    KNIGHT('N', 3),
    BISHOP('B', 3),
    QUEEN('Q', 9),
    KING('K', 0),
    PAWN('P', 1);

    private final char code;
    private final int materialValue;

    PieceKind(char code, int materialValue) {
        this.code = code;
        this.materialValue = materialValue;
    }

    public char code() {
        return code;
    }

    /** Standard chess point value, used for the captured-material score; a king is never scored. */
    public int materialValue() {
        return materialValue;
    }

    public static PieceKind fromCode(char code) {
        char upper = Character.toUpperCase(code);
        for (PieceKind kind : values()) {
            if (kind.code == upper) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown piece kind code: " + code);
    }
}
