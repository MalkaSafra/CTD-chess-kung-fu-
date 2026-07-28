package com.kungfuchess.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.game.DisconnectResignHandler.ReclaimedSeat;

class DisconnectResignHandlerTest {

    private RoomRegistry newRegistry() {
        return new RoomRegistry(new GameEventBus(event -> { }));
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }

    @Test
    void disconnectingASessionThatNeverJoinedARoomSchedulesNothing() {
        RoomRegistry roomRegistry = newRegistry();
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        DisconnectResignHandler handler = new DisconnectResignHandler(roomRegistry, scheduler);

        handler.onSessionDisconnect(disconnectEvent("session-1"));

        verifyNoInteractions(scheduler);
    }

    @Test
    void disconnectingASpectatorSchedulesNothingButRemovesThem() {
        RoomRegistry roomRegistry = newRegistry();
        roomRegistry.joinOrCreate("room-1", "s1", "alice");
        roomRegistry.joinOrCreate("room-1", "s2", "bob");
        roomRegistry.joinOrCreate("room-1", "s3", "carol"); // room already full -- joins as spectator
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        DisconnectResignHandler handler = new DisconnectResignHandler(roomRegistry, scheduler);

        handler.onSessionDisconnect(disconnectEvent("s3"));

        verifyNoInteractions(scheduler);
        assertNull(roomRegistry.roomForSession("s3"));
    }

    @Test
    void disconnectingASeatedPlayerSchedulesAnAutoResignAfterTheGracePeriod() {
        RoomRegistry roomRegistry = newRegistry();
        roomRegistry.joinOrCreate("room-1", "session-1", "alice");
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        DisconnectResignHandler handler = new DisconnectResignHandler(roomRegistry, scheduler);

        handler.onSessionDisconnect(disconnectEvent("session-1"));

        verify(scheduler).schedule(any(Runnable.class), eq(DisconnectResignHandler.GRACE_PERIOD_SECONDS),
                eq(TimeUnit.SECONDS));
    }

    @Test
    void ifTheGracePeriodElapsesWithoutAReconnectTheGameIsResignedToTheOpponent() {
        RoomRegistry roomRegistry = newRegistry();
        roomRegistry.joinOrCreate("room-1", "session-1", "alice");
        roomRegistry.joinOrCreate("room-1", "session-2", "bob");
        GameRoom room = roomRegistry.findById("room-1");
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        DisconnectResignHandler handler = new DisconnectResignHandler(roomRegistry, scheduler);

        handler.onSessionDisconnect(disconnectEvent("session-1"));
        runTheScheduledTask(scheduler);

        assertTrue(room.engine().snapshot(null).gameOver());
        assertEquals(PieceColor.BLACK, room.engine().snapshot(null).winner());
    }

    @Test
    void reconnectingBeforeTheGracePeriodElapsesCancelsTheAutoResignAndMovesTheSeat() {
        RoomRegistry roomRegistry = newRegistry();
        roomRegistry.joinOrCreate("room-1", "old-session", "alice");
        GameRoom room = roomRegistry.findById("room-1");
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> scheduledTask = mock(ScheduledFuture.class);
        doReturn(scheduledTask).when(scheduler).schedule(any(Runnable.class),
                eq(DisconnectResignHandler.GRACE_PERIOD_SECONDS), eq(TimeUnit.SECONDS));
        DisconnectResignHandler handler = new DisconnectResignHandler(roomRegistry, scheduler);
        handler.onSessionDisconnect(disconnectEvent("old-session"));

        ReclaimedSeat reclaimed = handler.tryReclaimSeat("alice", "new-session");

        assertEquals("room-1", reclaimed.roomId());
        assertEquals(PieceColor.WHITE, reclaimed.color());
        assertNull(room.colorOf("old-session"));
        assertEquals(PieceColor.WHITE, room.colorOf("new-session"));
        verify(scheduledTask).cancel(false);
        assertFalse(room.engine().snapshot(null).gameOver());
    }

    @Test
    void reconnectingUnderADifferentUsernameDoesNotReclaimAnything() {
        RoomRegistry roomRegistry = newRegistry();
        roomRegistry.joinOrCreate("room-1", "old-session", "alice");
        GameRoom room = roomRegistry.findById("room-1");
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        DisconnectResignHandler handler = new DisconnectResignHandler(roomRegistry, scheduler);
        handler.onSessionDisconnect(disconnectEvent("old-session"));

        ReclaimedSeat reclaimed = handler.tryReclaimSeat("someone-else", "new-session");

        assertNull(reclaimed);
        assertNull(room.colorOf("new-session"));
    }

    @Test
    void anOrdinaryLoginWithNothingDisconnectedReclaimsNothing() {
        RoomRegistry roomRegistry = newRegistry();
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        DisconnectResignHandler handler = new DisconnectResignHandler(roomRegistry, scheduler);

        assertNull(handler.tryReclaimSeat("alice", "session-1"));
    }

    private void runTheScheduledTask(ScheduledExecutorService scheduler) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(captor.capture(), eq(DisconnectResignHandler.GRACE_PERIOD_SECONDS),
                eq(TimeUnit.SECONDS));
        captor.getValue().run();
    }
}
