package com.kungfuchess.model;

public final class Selection {

    private final int row;
    private final int col;
    private final Piece piece;

    public Selection(int row, int col, Piece piece) {
        this.row = row;
        this.col = col;
        this.piece = piece;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Piece getPiece() {
        return piece;
    }
}
