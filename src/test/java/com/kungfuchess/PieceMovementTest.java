package com.kungfuchess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;

/**
 * Tests for each {@link Piece} subclass's geometric shape rule
 * ({@link Piece#isValidMoveShape}) and, where the shape alone can't decide
 * legality (path blocking, pawn direction/capture rules), the full
 * {@link Piece#isValidMove}. Every test builds its state directly through
 * the public {@link Board}/{@link Piece} API -- no reflection, no test
 * doubles.
 */
class PieceMovementTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(8, 8);
    }

    @Nested
    class KingMovement {

        private final Piece king = Piece.fromToken("wK");

        @Test
        void oneStepInAnyDirectionIsValid() {
            assertTrue(king.isValidMoveShape(3, 3, 3, 4)); // right
            assertTrue(king.isValidMoveShape(3, 3, 4, 3)); // down
            assertTrue(king.isValidMoveShape(3, 3, 4, 4)); // diagonal
            assertTrue(king.isValidMoveShape(3, 3, 2, 2)); // diagonal, opposite way
        }

        @Test
        void twoOrMoreStepsIsInvalid() {
            assertFalse(king.isValidMoveShape(3, 3, 3, 5));
            assertFalse(king.isValidMoveShape(3, 3, 5, 3));
            assertFalse(king.isValidMoveShape(3, 3, 5, 5));
        }

        @Test
        void stayingOnTheSameCellIsInvalidShape() {
            assertFalse(king.isValidMoveShape(3, 3, 3, 3));
        }
    }

    @Nested
    class QueenMovement {

        private final Piece queen = Piece.fromToken("wQ");

        @Test
        void anyStraightOrDiagonalDistanceIsValid() {
            assertTrue(queen.isValidMoveShape(3, 3, 3, 7)); // horizontal
            assertTrue(queen.isValidMoveShape(3, 3, 7, 3)); // vertical
            assertTrue(queen.isValidMoveShape(3, 3, 6, 6)); // diagonal
        }

        @Test
        void nonAlignedShapeIsInvalid() {
            assertFalse(queen.isValidMoveShape(3, 3, 5, 4));
        }
    }

    @Nested
    class RookMovement {

        private final Piece rook = Piece.fromToken("wR");

        @Test
        void straightLineIsValid() {
            assertTrue(rook.isValidMoveShape(0, 0, 0, 5));
            assertTrue(rook.isValidMoveShape(0, 0, 5, 0));
        }

        @Test
        void diagonalOrOffsetIsInvalid() {
            assertFalse(rook.isValidMoveShape(0, 0, 5, 5));
            assertFalse(rook.isValidMoveShape(0, 0, 2, 3));
        }

        @Test
        void pieceBlockingTheStraightPathMakesTheFullMoveInvalid() {
            board.setPiece(0, 0, Piece.fromToken("wR"));
            board.setPiece(0, 3, Piece.fromToken("wP"));
            Piece placedRook = board.getPiece(0, 0);
            assertFalse(placedRook.isValidMove(board, 0, 0, 0, 5));
        }
    }

    @Nested
    class BishopMovement {

        private final Piece bishop = Piece.fromToken("wB");

        @Test
        void diagonalIsValid() {
            assertTrue(bishop.isValidMoveShape(0, 0, 4, 4));
        }

        @Test
        void straightLineIsInvalid() {
            assertFalse(bishop.isValidMoveShape(0, 0, 0, 4));
            assertFalse(bishop.isValidMoveShape(0, 0, 4, 0));
        }
    }

    @Nested
    class KnightMovement {

        private final Piece knight = Piece.fromToken("wN");

        @Test
        void lShapeIsValid() {
            assertTrue(knight.isValidMoveShape(3, 3, 4, 5));
            assertTrue(knight.isValidMoveShape(3, 3, 5, 4));
        }

        @Test
        void nonLShapeIsInvalid() {
            assertFalse(knight.isValidMoveShape(3, 3, 4, 4)); // one-step diagonal
            assertFalse(knight.isValidMoveShape(3, 3, 5, 5)); // two-step diagonal
        }

        @Test
        void doesNotRequireAClearPathUnlikeSlidingPieces() {
            assertFalse(knight.requiresClearPath());
        }

        @Test
        void canJumpOverPiecesOccupyingWouldBeBlockingCells() {
            board.setPiece(0, 1, Piece.fromToken("wP"));
            board.setPiece(1, 0, Piece.fromToken("wP"));
            assertTrue(knight.isValidMove(board, 0, 0, 2, 1));
        }
    }

    @Nested
    class PawnMovement {

        @Test
        void whitePawnMovesUpwardOnly() {
            Piece whitePawn = Piece.fromToken("wP");
            board.setPiece(6, 0, whitePawn);
            assertTrue(whitePawn.isValidMove(board, 6, 0, 5, 0));
            assertFalse(whitePawn.isValidMove(board, 6, 0, 7, 0));
        }

        @Test
        void blackPawnMovesDownwardOnly() {
            Piece blackPawn = Piece.fromToken("bP");
            board.setPiece(1, 0, blackPawn);
            assertTrue(blackPawn.isValidMove(board, 1, 0, 2, 0));
            assertFalse(blackPawn.isValidMove(board, 1, 0, 0, 0));
        }

        @Test
        void validTwoCellMoveFromTheStartingRow() {
            Piece whitePawn = Piece.fromToken("wP");
            int startRow = board.getRows() - 1;
            board.setPiece(startRow, 3, whitePawn);
            assertTrue(whitePawn.isValidMove(board, startRow, 3, startRow - 2, 3));
        }

        @Test
        void twoCellMoveIsInvalidOnceOffTheStartingRow() {
            Piece whitePawn = Piece.fromToken("wP");
            int notStartRow = board.getRows() - 3;
            board.setPiece(notStartRow, 3, whitePawn);
            assertFalse(whitePawn.isValidMove(board, notStartRow, 3, notStartRow - 2, 3));
        }

        @Test
        void diagonalMoveOntoAnEmptyCellIsInvalid() {
            Piece whitePawn = Piece.fromToken("wP");
            board.setPiece(4, 4, whitePawn);
            assertFalse(whitePawn.isValidMove(board, 4, 4, 3, 5));
        }

        @Test
        void diagonalCaptureOfAnEnemyPieceIsValid() {
            Piece whitePawn = Piece.fromToken("wP");
            board.setPiece(4, 4, whitePawn);
            board.setPiece(3, 5, Piece.fromToken("bN"));
            assertTrue(whitePawn.isValidMove(board, 4, 4, 3, 5));
        }

        @Test
        void forwardCaptureIsInvalidEvenWithAnEnemyDirectlyAhead() {
            Piece whitePawn = Piece.fromToken("wP");
            board.setPiece(4, 4, whitePawn);
            board.setPiece(3, 4, Piece.fromToken("bN"));
            assertFalse(whitePawn.isValidMove(board, 4, 4, 3, 4));
        }
    }
}
