package com.kungfuchess.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

class RuleEngineTest {

    private Board board;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        board = new Board(8, 8);
        ruleEngine = new RuleEngine();
    }

    @Test
    void destinationOutsideBoardIsInvalid() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), PieceState.IDLE);
        board.addPiece(rook);

        MoveOutcome result = ruleEngine.validateMove(board, new Position(0, 0), new Position(8, 0));

        assertFalse(result.isAccepted());
        assertEquals(MoveRejection.OUTSIDE_BOARD, result.getReason());
    }

    @Test
    void movingFromAnEmptySquareIsInvalid() {
        MoveOutcome result = ruleEngine.validateMove(board, new Position(2, 2), new Position(3, 2));

        assertFalse(result.isAccepted());
        assertEquals(MoveRejection.EMPTY_SOURCE, result.getReason());
    }

    @Test
    void capturingAPieceOfTheSameColorIsInvalid() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), PieceState.IDLE);
        Piece ownPawn = new Piece(PieceColor.WHITE, PieceKind.PAWN, new Position(0, 4), PieceState.IDLE);
        board.addPiece(rook);
        board.addPiece(ownPawn);

        MoveOutcome result = ruleEngine.validateMove(board, new Position(0, 0), new Position(0, 4));

        assertFalse(result.isAccepted());
        assertEquals(MoveRejection.FRIENDLY_DESTINATION, result.getReason());
    }

    @Test
    void geometricallyIllegalMoveIsInvalid() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), PieceState.IDLE);
        board.addPiece(rook);

        MoveOutcome result = ruleEngine.validateMove(board, new Position(0, 0), new Position(1, 1));

        assertFalse(result.isAccepted());
        assertEquals(MoveRejection.ILLEGAL_PIECE_MOVE, result.getReason());
    }

    @Test
    void fullyValidMoveIsAccepted() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), PieceState.IDLE);
        board.addPiece(rook);

        MoveOutcome result = ruleEngine.validateMove(board, new Position(0, 0), new Position(0, 5));

        assertTrue(result.isAccepted());
        assertEquals(MoveRejection.NONE, result.getReason());
    }

    @Test
    void capturingAnEnemyPieceIsValid() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0), PieceState.IDLE);
        Piece enemyPawn = new Piece(PieceColor.BLACK, PieceKind.PAWN, new Position(0, 4), PieceState.IDLE);
        board.addPiece(rook);
        board.addPiece(enemyPawn);

        MoveOutcome result = ruleEngine.validateMove(board, new Position(0, 0), new Position(0, 4));

        assertTrue(result.isAccepted());
        assertEquals(MoveRejection.NONE, result.getReason());
    }
}
