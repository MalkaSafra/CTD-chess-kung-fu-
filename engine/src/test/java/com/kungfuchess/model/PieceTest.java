package com.kungfuchess.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PieceTest {

    private Piece piece(PieceState initialState) {
        return new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), initialState);
    }

    private Piece piece(PieceKind kind) {
        return new Piece(PieceColor.WHITE, kind, new Position(0, 0), PieceState.IDLE);
    }

    @Test
    void startMovingFromIdleTransitionsToMoving() {
        Piece piece = piece(PieceState.IDLE);

        piece.startMoving();

        assertEquals(PieceState.MOVING, piece.getState());
    }

    @Test
    void startMovingFromNonIdleThrows() {
        Piece piece = piece(PieceState.MOVING);

        assertThrows(IllegalStateException.class, piece::startMoving);
    }

    @Test
    void startJumpingFromIdleTransitionsToJumping() {
        Piece piece = piece(PieceState.IDLE);

        piece.startJumping();

        assertEquals(PieceState.JUMPING, piece.getState());
    }

    @Test
    void startJumpingFromNonIdleThrows() {
        Piece piece = piece(PieceState.SHORT_REST);

        assertThrows(IllegalStateException.class, piece::startJumping);
    }

    @Test
    void enterShortRestFromJumpingTransitionsToShortRest() {
        Piece piece = piece(PieceState.JUMPING);

        piece.enterShortRest();

        assertEquals(PieceState.SHORT_REST, piece.getState());
    }

    @Test
    void enterShortRestFromNonJumpingThrows() {
        Piece piece = piece(PieceState.IDLE);

        assertThrows(IllegalStateException.class, piece::enterShortRest);
    }

    @Test
    void enterLongRestFromMovingTransitionsToLongRest() {
        Piece piece = piece(PieceState.MOVING);

        piece.enterLongRest();

        assertEquals(PieceState.LONG_REST, piece.getState());
    }

    @Test
    void enterLongRestFromNonMovingThrows() {
        Piece piece = piece(PieceState.JUMPING);

        assertThrows(IllegalStateException.class, piece::enterLongRest);
    }

    @Test
    void wakeFromShortRestTransitionsToIdle() {
        Piece piece = piece(PieceState.SHORT_REST);

        piece.wake();

        assertEquals(PieceState.IDLE, piece.getState());
    }

    @Test
    void wakeFromLongRestTransitionsToIdle() {
        Piece piece = piece(PieceState.LONG_REST);

        piece.wake();

        assertEquals(PieceState.IDLE, piece.getState());
    }

    @Test
    void wakeFromNonRestingStateThrows() {
        Piece piece = piece(PieceState.IDLE);

        assertThrows(IllegalStateException.class, piece::wake);
    }

    @Test
    void captureIsLegalFromEveryNonCapturedState() {
        for (PieceState state : PieceState.values()) {
            if (state == PieceState.CAPTURED) {
                continue;
            }
            Piece piece = piece(state);

            piece.capture();

            assertEquals(PieceState.CAPTURED, piece.getState());
        }
    }

    @Test
    void promoteToQueenFromPawnTransitionsKindToQueen() {
        Piece piece = piece(PieceKind.PAWN);

        piece.promoteToQueen();

        assertEquals(PieceKind.QUEEN, piece.getKind());
    }

    @Test
    void promoteToQueenFromNonPawnThrows() {
        Piece piece = piece(PieceKind.ROOK);

        assertThrows(IllegalStateException.class, piece::promoteToQueen);
    }
}
