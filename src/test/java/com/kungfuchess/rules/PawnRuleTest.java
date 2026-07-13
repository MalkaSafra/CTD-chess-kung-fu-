package com.kungfuchess.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

class PawnRuleTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(8, 8);
    }

    @Test
    void whitePawnMovesOneSquareForwardWhenEmpty() {
        Piece pawn = new Piece(PieceColor.WHITE, PieceKind.PAWN, new Position(4, 4), PieceState.IDLE);
        board.addPiece(pawn);

        Set<Position> destinations = PieceRules.getLegalDestinations(board, pawn);

        assertEquals(1, destinations.size());
        assertTrue(destinations.contains(new Position(3, 4)));
    }

    @Test
    void whitePawnCannotAdvanceIntoAnOccupiedSquare() {
        Piece pawn = new Piece(PieceColor.WHITE, PieceKind.PAWN, new Position(4, 4), PieceState.IDLE);
        Piece blocker = new Piece(PieceColor.BLACK, PieceKind.PAWN, new Position(3, 4), PieceState.IDLE);
        board.addPiece(pawn);
        board.addPiece(blocker);

        Set<Position> destinations = PieceRules.getLegalDestinations(board, pawn);

        assertFalse(destinations.contains(new Position(3, 4)));
    }

    @Test
    void whitePawnCapturesDiagonallyOnlyWhenOccupied() {
        Piece pawn = new Piece(PieceColor.WHITE, PieceKind.PAWN, new Position(4, 4), PieceState.IDLE);
        Piece target = new Piece(PieceColor.BLACK, PieceKind.PAWN, new Position(3, 5), PieceState.IDLE);
        board.addPiece(pawn);
        board.addPiece(target);

        Set<Position> destinations = PieceRules.getLegalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(3, 5)), "diagonal square with a piece on it should be capturable");
        assertFalse(destinations.contains(new Position(3, 3)), "empty diagonal square should not be reachable");
    }

    @Test
    void blackPawnAdvancesTowardIncreasingRow() {
        Piece pawn = new Piece(PieceColor.BLACK, PieceKind.PAWN, new Position(3, 4), PieceState.IDLE);
        board.addPiece(pawn);

        Set<Position> destinations = PieceRules.getLegalDestinations(board, pawn);

        assertEquals(1, destinations.size());
        assertTrue(destinations.contains(new Position(4, 4)));
    }
}