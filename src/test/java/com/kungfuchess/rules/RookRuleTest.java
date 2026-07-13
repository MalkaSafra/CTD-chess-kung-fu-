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

class RookRuleTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(8, 8);
    }

    @Test
    void emptyBoardReachesFourteenSquares() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(4, 4), PieceState.IDLE);
        board.addPiece(rook);

        Set<Position> destinations = PieceRules.getLegalDestinations(board, rook);

        assertEquals(14, destinations.size());
        // up column
        assertTrue(destinations.contains(new Position(0, 4)));
        assertTrue(destinations.contains(new Position(3, 4)));
        // down column
        assertTrue(destinations.contains(new Position(7, 4)));
        assertTrue(destinations.contains(new Position(5, 4)));
        // left row
        assertTrue(destinations.contains(new Position(4, 0)));
        assertTrue(destinations.contains(new Position(4, 3)));
        // right row
        assertTrue(destinations.contains(new Position(4, 7)));
        assertTrue(destinations.contains(new Position(4, 5)));
    }

    @Test
    void reachStopsAtAndIncludesBlockingPiece() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(4, 4), PieceState.IDLE);
        Piece blocker = new Piece(PieceColor.BLACK, PieceKind.PAWN, new Position(4, 6), PieceState.IDLE);
        board.addPiece(rook);
        board.addPiece(blocker);

        Set<Position> destinations = PieceRules.getLegalDestinations(board, rook);

        assertTrue(destinations.contains(new Position(4, 5)));
        assertTrue(destinations.contains(new Position(4, 6)), "blocking square itself must be included");
        assertFalse(destinations.contains(new Position(4, 7)), "squares beyond the blocker must not be reachable");
    }
}