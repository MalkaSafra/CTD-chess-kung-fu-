package com.kungfuchess.server.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.RuleEngine;

class GameEventPublisherTest {

    private final List<Object> published = new ArrayList<>();
    private final GameEventBus bus = new GameEventBus(published::add);

    private GameEngine newEngine(Board board) {
        return new GameEngine(new GameState(board), new RuleEngine(), new RealTimeArbiter());
    }

    private Piece place(Board board, PieceColor color, PieceKind kind, Position position) {
        Piece piece = new Piece(color, kind, position, PieceState.IDLE);
        board.addPiece(piece);
        return piece;
    }

    @Test
    void firstAdvancePublishesGameStartedExactlyOnce() {
        GameEventPublisher publisher = new GameEventPublisher(newEngine(new Board(8, 8)), bus, "room-1");

        publisher.advance(100);
        publisher.advance(100);

        long gameStartedCount = published.stream().filter(GameStartedEvent.class::isInstance).count();
        assertEquals(1, gameStartedCount);
    }

    @Test
    void movingAPieceLogsAMoveEventImmediatelyOnRequest() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);
        new GameEventPublisher(engine, bus, "room-1");

        engine.requestMove(new Position(7, 0), new Position(7, 5));

        assertEquals(1, published.size());
        MoveLoggedEvent event = assertInstanceOf(MoveLoggedEvent.class, published.get(0));
        assertEquals(PieceColor.WHITE, event.record().color());
        assertEquals(PieceKind.ROOK, event.record().kind());
    }

    @Test
    void captureThatChangesScorePublishesScoreUpdatedForTheCapturingSide() {
        Board board = new Board(1, 4);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        place(board, PieceColor.BLACK, PieceKind.ROOK, new Position(0, 3));
        GameEngine engine = newEngine(board);
        GameEventPublisher publisher = new GameEventPublisher(engine, bus, "room-1");

        engine.requestMove(new Position(0, 0), new Position(0, 3)); // white captures black's rook
        publisher.advance(1000);

        List<ScoreUpdatedEvent> scoreEvents = published.stream()
                .filter(ScoreUpdatedEvent.class::isInstance)
                .map(ScoreUpdatedEvent.class::cast)
                .toList();

        assertEquals(1, scoreEvents.size());
        assertEquals(PieceColor.WHITE, scoreEvents.get(0).color());
        assertEquals(PieceKind.ROOK.materialValue(), scoreEvents.get(0).newScore());
    }

    @Test
    void kingCaptureEndsTheGameAndPublishesGameEndedWithTheWinner() {
        Board board = new Board(1, 4);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(0, 0));
        place(board, PieceColor.BLACK, PieceKind.KING, new Position(0, 3));
        GameEngine engine = newEngine(board);
        GameEventPublisher publisher = new GameEventPublisher(engine, bus, "room-1");

        engine.requestMove(new Position(0, 0), new Position(0, 3)); // white captures black's king
        publisher.advance(1000);

        List<GameEndedEvent> endEvents = published.stream()
                .filter(GameEndedEvent.class::isInstance)
                .map(GameEndedEvent.class::cast)
                .toList();

        assertEquals(1, endEvents.size());
        assertEquals(PieceColor.WHITE, endEvents.get(0).winner());
    }

    @Test
    void tickWithNoCaptureOrGameEndPublishesOnlyGameStarted() {
        Board board = new Board(8, 8);
        place(board, PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0));
        GameEngine engine = newEngine(board);
        GameEventPublisher publisher = new GameEventPublisher(engine, bus, "room-1");

        publisher.advance(100);

        assertEquals(1, published.size());
        assertTrue(published.get(0) instanceof GameStartedEvent);
    }
}
