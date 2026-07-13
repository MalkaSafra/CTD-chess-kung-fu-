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

    public static PieceColor fromCode(char c) {
        char lower = Character.toLowerCase(c);
        for (PieceColor color : values()) {
            if (color.code == lower) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color code: " + c);
    }
}
