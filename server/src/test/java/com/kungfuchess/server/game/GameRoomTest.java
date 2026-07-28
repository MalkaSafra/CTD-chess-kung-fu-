package com.kungfuchess.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.io.StandardBoard;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.RuleEngine;
import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.bus.GameEventPublisher;

class GameRoomTest {

    private GameRoom newRoom() {
        GameEngine engine = new GameEngine(new GameState(StandardBoard.create()), new RuleEngine(),
                new RealTimeArbiter());
        GameEventPublisher publisher = new GameEventPublisher(engine, new GameEventBus(event -> { }), "room-1");
        return new GameRoom("room-1", engine, publisher);
    }

    @Test
    void firstJoinerBecomesWhiteSecondBecomesBlackThirdBecomesSpectator() {
        GameRoom room = newRoom();

        assertEquals(RoomRole.WHITE, room.join("s1", "alice"));
        assertEquals(RoomRole.BLACK, room.join("s2", "bob"));
        assertEquals(RoomRole.SPECTATOR, room.join("s3", "carol"));

        assertEquals(PieceColor.WHITE, room.colorOf("s1"));
        assertEquals(PieceColor.BLACK, room.colorOf("s2"));
        assertNull(room.colorOf("s3"));
        assertFalse(room.hasOpenSeats());
    }

    @Test
    void rejoiningReportsTheSameRoleRatherThanGrantingAnother() {
        GameRoom room = newRoom();
        room.join("s1", "alice");

        assertEquals(RoomRole.WHITE, room.join("s1", "alice"));
    }

    @Test
    void assignSeatGrantsTheGivenColorDirectly() {
        GameRoom room = newRoom();

        room.assignSeat("s1", PieceColor.BLACK, "alice");

        assertEquals(PieceColor.BLACK, room.colorOf("s1"));
        assertEquals("alice", room.usernameForColor(PieceColor.BLACK));
    }

    @Test
    void reassignSeatMovesTheColorFromTheOldSessionToTheNewOne() {
        GameRoom room = newRoom();
        room.assignSeat("old-session", PieceColor.WHITE, "alice");

        room.reassignSeat("old-session", "new-session");

        assertNull(room.colorOf("old-session"));
        assertEquals(PieceColor.WHITE, room.colorOf("new-session"));
        assertTrue(room.hasOpenSeats(), "reassigning shouldn't change how many seats are occupied");
    }

    @Test
    void usernameForColorIsNullWhenNobodyHoldsThatSeat() {
        GameRoom room = newRoom();

        assertNull(room.usernameForColor(PieceColor.WHITE));
    }
}
