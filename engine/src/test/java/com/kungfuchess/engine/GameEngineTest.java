package com.kungfuchess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kungfuchess.config.GameConfig;
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

    @Test
    void simultaneousSwapCaptureAwardsTheWinToWhicheverSideMovedFirst_whiteFirst() {
        // Two rooks facing off on a 1x4 board, each attacking the other's square at the exact
        // same instant (both are fixed-duration capture motions started in the same tick).
        // White requested its capture first, so white should be the survivor at (0, 3), and
        // black's own in-flight "arrival" at (0, 0) must be discarded rather than completed --
        // otherwise the already-captured black piece gets resurrected there.
        Board board = new Board(1, 4);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        place(board, PieceColor.BLACK, PieceKind.ROOK, new Position(0, 3));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(0, 0), new Position(0, 3));
        engine.requestMove(new Position(0, 3), new Position(0, 0));

        engine.waitClock(3000);

        Piece survivor = board.getPiece(new Position(0, 3));
        assertEquals(PieceKind.ROOK, survivor.getKind());
        assertEquals(PieceColor.WHITE, survivor.getColor(), "white requested the swap-capture first and should win it");
        assertNull(board.getPiece(new Position(0, 0)), "the loser's square must stay empty, not be re-occupied by the captured piece");
    }

    @Test
    void simultaneousSwapCaptureAwardsTheWinToWhicheverSideMovedFirst_blackFirst() {
        Board board = new Board(1, 4);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        place(board, PieceColor.BLACK, PieceKind.ROOK, new Position(0, 3));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(0, 3), new Position(0, 0));
        engine.requestMove(new Position(0, 0), new Position(0, 3));

        engine.waitClock(3000);

        Piece survivor = board.getPiece(new Position(0, 0));
        assertEquals(PieceKind.ROOK, survivor.getKind());
        assertEquals(PieceColor.BLACK, survivor.getColor(), "black requested the swap-capture first and should win it");
        assertNull(board.getPiece(new Position(0, 3)), "the loser's square must stay empty, not be re-occupied by the captured piece");
    }

    @Test
    void pawnReachingTheBackRankIsPromotedToQueenAfterItArrives() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.PAWN, new Position(1, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(1, 0), new Position(0, 0));
        engine.waitClock(1000);

        Piece promoted = board.getPiece(new Position(0, 0));
        assertEquals(PieceKind.QUEEN, promoted.getKind());
        assertEquals(PieceColor.WHITE, promoted.getColor());
    }

    @Test
    void snapshotIncludesPawnPromotedEventOnceThenClearsItOnTheNextSnapshot() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.PAWN, new Position(1, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(1, 0), new Position(0, 0));
        engine.waitClock(1000);

        assertEquals(List.of(TransientEvent.PAWN_PROMOTED), engine.snapshot(null).events());
        assertEquals(List.of(), engine.snapshot(null).events(), "already reported once; must not repeat on the next read");
    }

    @Test
    void pawnPromotionNotifiesPromotionListenerWithPositionAndColor() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.PAWN, new Position(1, 0));
        GameEngine engine = newEngine(board);

        List<Position> promotedPositions = new ArrayList<>();
        List<PieceColor> promotedColors = new ArrayList<>();
        engine.addPromotionListener((position, color) -> {
            promotedPositions.add(position);
            promotedColors.add(color);
        });

        engine.requestMove(new Position(1, 0), new Position(0, 0));
        engine.waitClock(1000);

        assertEquals(List.of(new Position(0, 0)), promotedPositions);
        assertEquals(List.of(PieceColor.WHITE), promotedColors);
    }

    @Test
    void pieceRestsAfterCompletingAMoveAndCanActOnceRestExpires() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(7, 0), new Position(7, 1)); // 1 cell -> 1000ms
        engine.waitClock(1000); // arrives, enters LONG_REST

        MoveOutcome tooSoon = engine.requestMove(new Position(7, 1), new Position(7, 2));
        assertFalse(tooSoon.isAccepted());
        assertEquals(MoveRejection.PIECE_RESTING, tooSoon.getReason());

        engine.waitClock(GameConfig.LONG_REST_DURATION_MS);

        MoveOutcome afterRest = engine.requestMove(new Position(7, 1), new Position(7, 2));
        assertTrue(afterRest.isAccepted());
    }

    @Test
    void pieceRestsAfterLandingAJumpAndCanActOnceRestExpires() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        engine.requestJump(new Position(7, 0));
        engine.waitClock(GameConfig.JUMP_DURATION_MS); // lands, enters SHORT_REST

        MoveOutcome tooSoon = engine.requestMove(new Position(7, 0), new Position(7, 1));
        assertFalse(tooSoon.isAccepted());
        assertEquals(MoveRejection.PIECE_RESTING, tooSoon.getReason());

        engine.waitClock(GameConfig.SHORT_REST_DURATION_MS);

        MoveOutcome afterRest = engine.requestMove(new Position(7, 0), new Position(7, 1));
        assertTrue(afterRest.isAccepted());
    }

    @Test
    void resigningEndsTheGameInTheOpponentsFavor() {
        GameEngine engine = newEngine(new Board(8, 8));

        engine.resign(PieceColor.WHITE);

        GameSnapshot snapshot = engine.snapshot(null);
        assertTrue(snapshot.gameOver());
        assertEquals(PieceColor.BLACK, snapshot.winner());
    }

    @Test
    void resigningAfterTheGameIsAlreadyOverDoesNotChangeTheWinner() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.KING, new Position(7, 4));
        place(board, PieceColor.BLACK, PieceKind.ROOK, new Position(6, 4));
        GameEngine engine = newEngine(board);
        engine.requestMove(new Position(6, 4), new Position(7, 4)); // black captures white's king
        engine.waitClock(GameConfig.CAPTURE_DURATION_MS + 1);
        assertTrue(engine.snapshot(null).gameOver());

        engine.resign(PieceColor.BLACK); // black tries to resign after already winning

        assertEquals(PieceColor.BLACK, engine.snapshot(null).winner());
    }

    @Test
    void movingAfterAResignationIsRejectedAsGameOver() {
        Board board = new Board(8, 8);
        place(board, PieceColor.BLACK, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);

        engine.resign(PieceColor.WHITE);

        MoveOutcome outcome = engine.requestMove(new Position(7, 0), new Position(7, 5));
        assertFalse(outcome.isAccepted());
        assertEquals(MoveRejection.GAME_OVER, outcome.getReason());
    }
}
