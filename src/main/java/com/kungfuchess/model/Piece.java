package com.kungfuchess.model;

public class Piece {

    private final PieceColor color;
    private PieceKind kind;
    private Position position;
    private PieceState state;

    public Piece(PieceColor color, PieceKind kind, Position position, PieceState state) {
        this.color = color;
        this.kind = kind;
        this.position = position;
        this.state = state;
    }

    public PieceColor getColor() {
        return color;
    }

    public PieceKind getKind() {
        return kind;
    }

    public void setKind(PieceKind kind) {
        this.kind = kind;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public PieceState getState() {
        return state;
    }

    public void setState(PieceState state) {
        this.state = state;
    }
}
