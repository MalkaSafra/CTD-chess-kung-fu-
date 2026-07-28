package com.kungfuchess.server.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.bus.GameEventBus;

class GameClockTest {

    /** A {@link Clock} the test can move forward on demand, mirroring {@code MatchmakingQueueTest}. */
    private static final class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void firstTickForARoomAlwaysBroadcastsImmediately() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        roomRegistry.joinOrCreate("R1", "s1", "alice");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        GameClock gameClock = new GameClock(roomRegistry, messagingTemplate, new ManualClock());

        gameClock.tick();

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/R1"), any(GameSnapshot.class));
    }

    @Test
    void ticksWithinTheThrottleWindowDoNotRebroadcast() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        roomRegistry.joinOrCreate("R1", "s1", "alice");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ManualClock clock = new ManualClock();
        GameClock gameClock = new GameClock(roomRegistry, messagingTemplate, clock);
        gameClock.tick(); // first tick always broadcasts

        clock.advanceMillis(GameClock.BROADCAST_INTERVAL_MS - 10);
        gameClock.tick();
        gameClock.tick();

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/R1"), any(GameSnapshot.class));
    }

    @Test
    void aTickAfterTheThrottleWindowElapsesBroadcastsAgain() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        roomRegistry.joinOrCreate("R1", "s1", "alice");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ManualClock clock = new ManualClock();
        GameClock gameClock = new GameClock(roomRegistry, messagingTemplate, clock);
        gameClock.tick();

        clock.advanceMillis(GameClock.BROADCAST_INTERVAL_MS);
        gameClock.tick();

        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/game/R1"), any(GameSnapshot.class));
    }

    @Test
    void aFinishedGameBroadcastsItsFinalStateImmediatelyEvenInsideTheThrottleWindow() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        GameRoom room = roomRegistry.createRoomForMatch("s1", "alice", "s2", "bob");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ManualClock clock = new ManualClock();
        GameClock gameClock = new GameClock(roomRegistry, messagingTemplate, clock);
        gameClock.tick(); // first tick, broadcasts once

        room.engine().resign(PieceColor.WHITE); // ends the game immediately, well inside the throttle window
        gameClock.tick();

        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/game/" + room.roomId()), any(GameSnapshot.class));
    }

    @Test
    void aFinishedRoomIsSkippedEntirelyOnLaterTicks() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        GameRoom room = roomRegistry.createRoomForMatch("s1", "alice", "s2", "bob");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ManualClock clock = new ManualClock();
        GameClock gameClock = new GameClock(roomRegistry, messagingTemplate, clock);
        room.engine().resign(PieceColor.WHITE);
        gameClock.tick(); // broadcasts the final state and marks the room finished

        clock.advanceMillis(10_000);
        gameClock.tick();
        gameClock.tick();

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/" + room.roomId()), any(GameSnapshot.class));
    }

    @Test
    void eachRoomHasItsOwnIndependentThrottle() {
        RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
        roomRegistry.joinOrCreate("R1", "s1", "alice");
        roomRegistry.joinOrCreate("R2", "s2", "bob");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        GameClock gameClock = new GameClock(roomRegistry, messagingTemplate, new ManualClock());

        gameClock.tick();

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/R1"), any(GameSnapshot.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/R2"), any(GameSnapshot.class));
    }
}
