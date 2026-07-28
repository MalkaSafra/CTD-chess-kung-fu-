package com.kungfuchess.server.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.game.RoomRegistry;
import com.kungfuchess.server.game.RoomRole;
import com.kungfuchess.server.game.SessionAccountRegistry;

class RoomControllerTest {

    private RoomController newController(RoomRegistry roomRegistry, SessionAccountRegistry sessionAccountRegistry) {
        return new RoomController(roomRegistry, sessionAccountRegistry);
    }

    @Test
    void firstAndSecondJoinerBecomeWhiteAndBlack() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "alice");
        sessionAccountRegistry.recordLogin("s2", "bob");
        RoomController controller = newController(roomRegistry, sessionAccountRegistry);

        RoomJoinResult first = controller.handleJoin(new RoomJoinCommand("ABC123"), "s1");
        RoomJoinResult second = controller.handleJoin(new RoomJoinCommand("ABC123"), "s2");

        assertEquals(RoomRole.WHITE, first.role());
        assertEquals(RoomRole.BLACK, second.role());
        assertEquals(first.roomId(), second.roomId());
        assertEquals("alice", second.whiteUsername(), "the second joiner should be told who's already there");
    }

    @Test
    void aThirdJoinerBecomesASpectatorAndSeesBothUsernames() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "alice");
        sessionAccountRegistry.recordLogin("s2", "bob");
        sessionAccountRegistry.recordLogin("s3", "carol");
        RoomController controller = newController(roomRegistry, sessionAccountRegistry);
        controller.handleJoin(new RoomJoinCommand("ABC123"), "s1");
        controller.handleJoin(new RoomJoinCommand("ABC123"), "s2");

        RoomJoinResult spectatorResult = controller.handleJoin(new RoomJoinCommand("ABC123"), "s3");

        assertEquals(RoomRole.SPECTATOR, spectatorResult.role());
        assertEquals("alice", spectatorResult.whiteUsername());
        assertEquals("bob", spectatorResult.blackUsername());
    }

    @Test
    void joiningBeforeLoggingInFails() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        RoomController controller = newController(roomRegistry, sessionAccountRegistry);

        RoomJoinResult result = controller.handleJoin(new RoomJoinCommand("ABC123"), "stranger-session");

        assertFalse(result.success());
        assertNull(result.roomId());
    }

    @Test
    void anEmptyRoomCodeFails() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "alice");
        RoomController controller = newController(roomRegistry, sessionAccountRegistry);

        RoomJoinResult result = controller.handleJoin(new RoomJoinCommand("   "), "s1");

        assertFalse(result.success());
    }

    @Test
    void twoDifferentCodesAreTwoIndependentRooms() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "alice");
        sessionAccountRegistry.recordLogin("s2", "bob");
        RoomController controller = newController(roomRegistry, sessionAccountRegistry);

        RoomJoinResult first = controller.handleJoin(new RoomJoinCommand("ROOM-A"), "s1");
        RoomJoinResult second = controller.handleJoin(new RoomJoinCommand("ROOM-B"), "s2");

        assertTrue(!first.roomId().equals(second.roomId()));
        assertEquals(RoomRole.WHITE, first.role());
        assertEquals(RoomRole.WHITE, second.role(), "a fresh room's first joiner is WHITE regardless of the other room");
    }
}
