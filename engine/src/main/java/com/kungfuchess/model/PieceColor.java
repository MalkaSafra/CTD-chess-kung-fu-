package com.kungfuchess.model;

public enum PieceColor {
    WHITE('w'),
    BLACK('b');

    private final char code;

    PieceColor(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public static PieceColor fromCode(char code) {
        char lower = Character.toLowerCase(code);
        for (PieceColor color : values()) {
            if (color.code == lower) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color code: " + code);
    }
}
