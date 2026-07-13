package com.kungfuchess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.MoveOutcome;
import com.kungfuchess.rules.MoveRejection;
import com.kungfuchess.rules.RuleEngine;

class GameEngineTest {

    private GameEngine newEngine(Board board) {
        return new GameEngine(new GameState(board), new RuleEngine(), new RealTimeArbiter());
    }

    private Piece place(Board board, PieceColor color, PieceKind kind, Position position) {
        Piece piece = new Piece(color, kind, position, PieceState.IDLE);
        board.addPiece(piece);
        return piece;
    }

    @Test
    void secondMoveRequestForAPieceAlreadyInFlightIsRejected() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        MoveOutcome first = engine.requestMove(new Position(7, 0), new Position(7, 5));
        assertTrue(first.isAccepted());

        MoveOutcome second = engine.requestMove(new Position(7, 0), new Position(7, 6));
        assertFalse(second.isAccepted());
        assertEquals(MoveRejection.PIECE_ALREADY_MOVING, second.getReason());
    }

    @Test
    void jumpRequestForAPieceAlreadyMovingIsRejected() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(7, 0), new Position(7, 5));
        MoveOutcome jump = engine.requestJump(new Position(7, 0));

        assertFalse(jump.isAccepted());
        assertEquals(MoveRejection.PIECE_ALREADY_MOVING, jump.getReason());
    }

    @Test
    void moveRequestForAPieceAlreadyJumpingIsRejected() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        engine.requestJump(new Position(7, 0));
        MoveOutcome move = engine.requestMove(new Position(7, 0), new Position(7, 5));

        assertFalse(move.isAccepted());
        assertEquals(MoveRejection.PIECE_ALREADY_MOVING, move.getReason());
    }

    @Test
    void rejectedMidFlightMoveDoesNotDisruptTheOngoingMotion() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(7, 0), new Position(7, 5));
        engine.requestMove(new Position(7, 0), new Position(7, 6)); // rejected; must not alter the motion above

        engine.waitClock(5000);

        assertEquals(PieceKind.ROOK, board.getPiece(new Position(7, 5)).getKind());
        assertNull(board.getPiece(new Position(7, 0)));
    }

    @Test
    void laterEnemyArrivalCapturesEarlierEnemyArrivalOnAnEmptySquare() {
        // Unequal distances (and therefore unequal durations) so a single wait() brings both
        // motions to "arrived" in the same tick with genuinely different overshoot: white's
        // 3-cell move finishes with time to spare (large overshoot, i.e. earlier), black's
        // 5-cell move finishes right at the wire (near-zero overshoot, i.e. later).
        Board board = new Board(10, 10);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(4, 1));
        place(board, PieceColor.BLACK, PieceKind.ROOK, new Position(4, 9));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(4, 1), new Position(4, 4));
        engine.requestMove(new Position(4, 9), new Position(4, 4));

        engine.waitClock(5000);

        Piece survivor = board.getPiece(new Position(4, 4));
        assertEquals(PieceKind.ROOK, survivor.getKind());
        assertEquals(PieceColor.BLACK, survivor.getColor(), "the piece that arrived later (less overshoot) should win");
        assertNull(board.getPiece(new Position(4, 1)), "the earlier (white) arriver must be captured and removed");
    }

    @Test
    void slidingPieceStopsOneSquareShortOfAFriendlyItCatchesUpTo() {
        // Mirrors the canonical example: a friendly Queen settles on e4 first; a Rook sliding
        // through that same square (e1->e8) later must stop at e3, one square short, not land on
        // or pass through its own ally. Here rank/file are just rows/cols: row 7 stands in for
        // the shared line, col 4 for "e", col 0/7 for "a"/"h".
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.QUEEN, new Position(6, 4));
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(6, 4), new Position(7, 4)); // queen: 1 cell, 1000ms
        engine.requestMove(new Position(7, 0), new Position(7, 7)); // rook: 7 cells, 7000ms

        engine.waitClock(1000); // queen arrives and settles at (7, 4) first
        engine.waitClock(6000); // rook's elapsed now reaches the (7, 4) cell -- but it's occupied

        assertEquals(PieceKind.QUEEN, board.getPiece(new Position(7, 4)).getKind(), "the queen holds its square");
        assertEquals(PieceKind.ROOK, board.getPiece(new Position(7, 3)).getKind(), "the rook stops one square short");
        assertNull(board.getPiece(new Position(7, 0)));
        assertNull(board.getPiece(new Position(7, 5)));
        assertNull(board.getPiece(new Position(7, 6)));
        assertNull(board.getPiece(new Position(7, 7)));
    }
}
