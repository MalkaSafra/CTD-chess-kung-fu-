package com.kungfuchess.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.bus.GameEventBus;

class RoomRegistryTest {

    private RoomRegistry newRegistry() {
        return new RoomRegistry(new GameEventBus(event -> { }));
    }

    @Test
    void joinOrCreateMakesARoomOnFirstUseThenReusesItByCode() {
        RoomRegistry registry = newRegistry();

        RoomRole first = registry.joinOrCreate("ABC123", "s1", "alice");
        RoomRole second = registry.joinOrCreate("ABC123", "s2", "bob");

        assertEquals(RoomRole.WHITE, first);
        assertEquals(RoomRole.BLACK, second);
        assertSame(registry.findById("ABC123"), registry.roomForSession("s1"));
    }

    @Test
    void aThirdJoinerOfAFullRoomBecomesASpectator() {
        RoomRegistry registry = newRegistry();
        registry.joinOrCreate("ABC123", "s1", "alice");
        registry.joinOrCreate("ABC123", "s2", "bob");

        RoomRole role = registry.joinOrCreate("ABC123", "s3", "carol");

        assertEquals(RoomRole.SPECTATOR, role);
    }

    @Test
    void twoDifferentCodesProduceTwoIndependentRooms() {
        RoomRegistry registry = newRegistry();

        registry.joinOrCreate("ROOM-A", "s1", "alice");
        registry.joinOrCreate("ROOM-B", "s2", "bob");

        assertNotEquals(registry.findById("ROOM-A"), registry.findById("ROOM-B"));
        assertEquals(PieceColor.WHITE, registry.findById("ROOM-A").colorOf("s1"));
        assertNull(registry.findById("ROOM-B").colorOf("s1"));
    }

    @Test
    void createRoomForMatchSeatsBothSessionsInAFreshIsolatedRoom() {
        RoomRegistry registry = newRegistry();

        GameRoom room = registry.createRoomForMatch("s1", "alice", "s2", "bob");

        assertNotNull(room.roomId());
        assertEquals(PieceColor.WHITE, room.colorOf("s1"));
        assertEquals(PieceColor.BLACK, room.colorOf("s2"));
        assertSame(room, registry.roomForSession("s1"));
        assertSame(room, registry.roomForSession("s2"));
    }

    @Test
    void roomForSessionIsNullForASessionThatNeverJoinedAnything() {
        RoomRegistry registry = newRegistry();

        assertNull(registry.roomForSession("stranger"));
    }

    @Test
    void moveSessionTransfersTheRoomMappingToTheNewSessionId() {
        RoomRegistry registry = newRegistry();
        registry.joinOrCreate("ABC123", "old-session", "alice");

        registry.moveSession("old-session", "new-session");

        assertNull(registry.roomForSession("old-session"));
        assertSame(registry.findById("ABC123"), registry.roomForSession("new-session"));
    }
}
