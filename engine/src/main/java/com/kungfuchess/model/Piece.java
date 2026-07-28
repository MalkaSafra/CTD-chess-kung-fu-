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

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public PieceState getState() {
        return state;
    }

    /** Begins a translation (a plain move or a capture). */
    public void startMoving() {
        requireState(PieceState.IDLE);
        state = PieceState.MOVING;
    }

    /** Begins a jump. */
    public void startJumping() {
        requireState(PieceState.IDLE);
        state = PieceState.JUMPING;
    }

    /** A jump has landed with nothing contesting it. */
    public void enterShortRest() {
        requireState(PieceState.JUMPING);
        state = PieceState.SHORT_REST;
    }

    /** A translation has completed -- either it arrived, or it was stopped short by a block. */
    public void enterLongRest() {
        requireState(PieceState.MOVING);
        state = PieceState.LONG_REST;
    }

    /** A rest period has timed out. */
    public void wake() {
        requireOneOf(PieceState.SHORT_REST, PieceState.LONG_REST);
        state = PieceState.IDLE;
    }

    /**
     * Removed from play by an opposing piece. Legal from any state -- a piece can be captured
     * while idle, mid-translation, mid-jump, or resting.
     */
    public void capture() {
        state = PieceState.CAPTURED;
    }

    /** A pawn that has reached the far edge of the board becomes a queen. */
    public void promoteToQueen() {
        requireKind(PieceKind.PAWN);
        kind = PieceKind.QUEEN;
    }

    private void requireState(PieceState expected) {
        if (state != expected) {
            throw new IllegalStateException("Expected state " + expected + " but piece was " + state);
        }
    }

    private void requireKind(PieceKind expected) {
        if (kind != expected) {
            throw new IllegalStateException("Expected kind " + expected + " but piece was " + kind);
        }
    }

    private void requireOneOf(PieceState... expected) {
        for (PieceState candidate : expected) {
            if (state == candidate) {
                return;
            }
        }
        throw new IllegalStateException("Expected one of " + java.util.Arrays.toString(expected)
                + " but piece was " + state);
    }
}
