package com.kungfuchess.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(8, 8);
    }

    @Test
    void addPiecePlacesItAtItsPosition() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), PieceState.IDLE);

        board.addPiece(rook);

        assertEquals(rook, board.getPiece(new Position(0, 0)));
    }

    @Test
    void addPieceOnOccupiedSquareThrows() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), PieceState.IDLE);
        Piece knight = new Piece(PieceColor.WHITE, PieceKind.KNIGHT, new Position(0, 0), PieceState.IDLE);
        board.addPiece(rook);

        assertThrows(IllegalStateException.class, () -> board.addPiece(knight));
        assertEquals(rook, board.getPiece(new Position(0, 0)));
    }

    @Test
    void addPieceOutOfBoundsThrows() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(8, 0), PieceState.IDLE);

        assertThrows(IllegalArgumentException.class, () -> board.addPiece(rook));
    }

    @Test
    void removePieceClearsTheSquareAndReturnsIt() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(3, 3), PieceState.IDLE);
        board.addPiece(rook);

        Piece removed = board.removePiece(new Position(3, 3));

        assertEquals(rook, removed);
        assertNull(board.getPiece(new Position(3, 3)));
    }

    @Test
    void getPieceOnEmptySquareReturnsNull() {
        assertNull(board.getPiece(new Position(5, 5)));
    }

    @Test
    void isInBoundsAcceptsAllEdgeCoordinates() {
        assertTrue(board.isInBounds(new Position(0, 0)));
        assertTrue(board.isInBounds(new Position(0, 7)));
        assertTrue(board.isInBounds(new Position(7, 0)));
        assertTrue(board.isInBounds(new Position(7, 7)));
    }

    @Test
    void isInBoundsRejectsCoordinatesOutsideTheGrid() {
        assertFalse(board.isInBounds(new Position(-1, 0)));
        assertFalse(board.isInBounds(new Position(0, -1)));
        assertFalse(board.isInBounds(new Position(8, 0)));
        assertFalse(board.isInBounds(new Position(0, 8)));
    }
}
