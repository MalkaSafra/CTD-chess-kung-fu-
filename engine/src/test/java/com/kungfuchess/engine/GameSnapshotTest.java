package com.kungfuchess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.RuleEngine;

class GameSnapshotTest {

    private GameEngine newEngine(Board board) {
        return new GameEngine(new GameState(board), new RuleEngine(), new RealTimeArbiter());
    }

    private Piece place(Board board, PieceColor color, PieceKind kind, Position position) {
        Piece piece = new Piece(color, kind, position, PieceState.IDLE);
        board.addPiece(piece);
        return piece;
    }

    private PieceSnapshot onlyPiece(GameSnapshot snapshot) {
        List<PieceSnapshot> pieces = snapshot.pieces();
        assertEquals(1, pieces.size());
        return pieces.get(0);
    }

    @Test
    void idlePieceSnapshotSitsAtItsOwnCell() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(2, 3));
        GameEngine engine = newEngine(board);

        PieceSnapshot snapshot = onlyPiece(engine.snapshot(null));

        assertEquals(300, snapshot.pixelX());
        assertEquals(200, snapshot.pixelY());
        assertEquals(PieceState.IDLE, snapshot.state());
        assertEquals(0, snapshot.stateElapsedMillis());
    }

    @Test
    void movingPieceSnapshotInterpolatesBetweenSourceAndDestination() {
        // Mirrors the reference example: 2-cell move (2000ms total), 500ms elapsed -> 25% there.
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(0, 0), new Position(0, 2));
        engine.waitClock(500);

        PieceSnapshot snapshot = onlyPiece(engine.snapshot(null));

        assertEquals(PieceState.MOVING, snapshot.state());
        assertEquals(50, snapshot.pixelX());
        assertEquals(0, snapshot.pixelY());
        assertEquals(500, snapshot.stateElapsedMillis());
    }

    @Test
    void jumpingPieceSnapshotStaysOnItsOwnCellWhileAirborne() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(2, 3));
        GameEngine engine = newEngine(board);

        engine.requestJump(new Position(2, 3));
        engine.waitClock(400);

        PieceSnapshot snapshot = onlyPiece(engine.snapshot(null));

        assertEquals(PieceState.JUMPING, snapshot.state());
        assertEquals(300, snapshot.pixelX());
        assertEquals(200, snapshot.pixelY());
        assertEquals(400, snapshot.stateElapsedMillis());
    }

    @Test
    void restingPieceSnapshotReportsElapsedTimeSinceRestBegan() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1)); // 1 cell -> 1000ms
        engine.waitClock(1000); // arrives, enters LONG_REST fresh
        engine.waitClock(300); // 300ms into the 1000ms long rest

        PieceSnapshot snapshot = onlyPiece(engine.snapshot(null));

        assertEquals(PieceState.LONG_REST, snapshot.state());
        assertEquals(100, snapshot.pixelX(), "piece has already landed on its new cell");
        assertEquals(300, snapshot.stateElapsedMillis());
    }

    @Test
    void capturingAPawnCreditsTheCapturingColorsScore() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        place(board, PieceColor.BLACK, PieceKind.PAWN, new Position(0, 2));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(0, 0), new Position(0, 2)); // capture -> CAPTURE_DURATION_MS
        engine.waitClock(1000);

        GameSnapshot snapshot = engine.snapshot(null);
        assertEquals(1, snapshot.whiteScore(), "white captured a pawn, worth 1 point");
        assertEquals(0, snapshot.blackScore());
    }

    @Test
    void capturingTheKingEndsTheGameAndRecordsTheWinner() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        place(board, PieceColor.BLACK, PieceKind.KING, new Position(0, 2));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(0, 0), new Position(0, 2));
        engine.waitClock(1000);

        GameSnapshot snapshot = engine.snapshot(null);
        assertTrue(snapshot.gameOver());
        assertEquals(PieceColor.WHITE, snapshot.winner());
        assertEquals(0, snapshot.whiteScore(), "a captured king carries no material value");
    }

    @Test
    void acceptedMovesAndJumpsAppearInMoveHistoryWithGameTimeTimestamps() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        place(board, PieceColor.BLACK, PieceKind.KNIGHT, new Position(0, 1));
        GameEngine engine = newEngine(board);

        engine.requestMove(new Position(7, 0), new Position(7, 2)); // recorded at t=0
        engine.waitClock(250);
        engine.requestJump(new Position(0, 1)); // recorded at t=250

        List<MoveRecord> history = engine.snapshot(null).moveHistory();

        assertEquals(2, history.size());

        MoveRecord move = history.get(0);
        assertEquals(PieceColor.WHITE, move.color());
        assertEquals(PieceKind.ROOK, move.kind());
        assertEquals(new Position(7, 0), move.source());
        assertEquals(new Position(7, 2), move.destination());
        assertEquals(0, move.timestampMs());
        assertFalse(move.isJump());

        MoveRecord jump = history.get(1);
        assertEquals(PieceColor.BLACK, jump.color());
        assertEquals(new Position(0, 1), jump.source());
        assertEquals(250, jump.timestampMs());
        assertTrue(jump.isJump());
    }

    @Test
    void selectingAPieceHighlightsItsLegalDestinations() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(4, 4));
        GameEngine engine = newEngine(board);

        GameSnapshot snapshot = engine.snapshot(new Position(4, 4));

        assertEquals(14, snapshot.legalDestinations().size());
        assertTrue(snapshot.legalDestinations().contains(new Position(4, 0)));
        assertTrue(snapshot.legalDestinations().contains(new Position(0, 4)));
    }

    @Test
    void legalDestinationsExcludeSquaresHeldByTheSameSide() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(4, 4));
        place(board, PieceColor.WHITE, PieceKind.PAWN, new Position(4, 6));
        GameEngine engine = newEngine(board);

        GameSnapshot snapshot = engine.snapshot(new Position(4, 4));

        assertFalse(snapshot.legalDestinations().contains(new Position(4, 6)),
                "a friendly-occupied square is never a legal move destination");
    }

    @Test
    void noSelectionMeansNoHighlightedDestinations() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(4, 4));
        GameEngine engine = newEngine(board);

        assertTrue(engine.snapshot(null).legalDestinations().isEmpty());
    }

    @Test
    void selectedPositionAndGameOverPassThroughUnchanged() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        GameEngine engine = newEngine(board);

        GameSnapshot snapshot = engine.snapshot(new Position(0, 0));

        assertEquals(new Position(0, 0), snapshot.selectedPosition());
        assertFalse(snapshot.gameOver());
        assertEquals(8, snapshot.boardRows());
        assertEquals(8, snapshot.boardCols());
    }
}
