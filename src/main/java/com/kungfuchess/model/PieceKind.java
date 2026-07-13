package com.kungfuchess.model;

public enum PieceKind {
    ROOK('R'),
    KNIGHT('N'),
    BISHOP('B'),
    QUEEN('Q'),
    KING('K'),
    PAWN('P');

    private final char code;

    PieceKind(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public static PieceKind fromCode(char c) {
        char upper = Character.toUpperCase(c);
        for (PieceKind kind : values()) {
            if (kind.code == upper) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown piece kind code: " + c);
    }
}
