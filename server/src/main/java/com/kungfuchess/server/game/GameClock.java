package com.kungfuchess.server.game;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.server.bus.GameEventPublisher;

/**
 * Drives every active room's real-time clock -- the server-side replacement for the GUI's Swing
 * {@code Timer} -- and broadcasts each room's resulting state to that room's own subscribers.
 * Advancing through {@link GameEventPublisher} (rather than calling {@code engine.waitClock}
 * directly) is what makes the pub/sub bus from Stage 1 actually fire during real play.
 *
 * <p>Two throttles keep a server running many concurrent rooms from flooding every client's
 * WebSocket and, in turn, its Swing Event Dispatch Thread (each incoming snapshot triggers a
 * {@code SwingUtilities.invokeLater}-queued re-render; deliver those faster than the EDT can draw
 * them and the queue backs up, which is what made clicks stop registering once several rooms were
 * live at once): each room's own engine is still advanced every {@link #TICK_MS} (16ms) --
 * changing that would desync real-time collision/contested-arrival resolution, which depends on
 * tick granularity -- but a room only gets a fresh broadcast at most every {@link
 * #BROADCAST_INTERVAL_MS}. (A tighter cap, e.g. once a second, was considered, but {@code
 * GameSnapshot} pixel positions are already server-interpolated for mid-flight pieces -- see
 * {@code SnapshotFactory} -- so a mid-move piece would visibly teleport between rare updates
 * instead of gliding; ~20fps is a large enough cut from 60fps while still reading as smooth
 * motion.) Once a room's game ends, its final snapshot is broadcast immediately regardless of the
 * throttle, and the room is then skipped entirely on every later tick -- there's nothing left to
 * advance or send, so a server that accumulates finished/abandoned rooms over a long uptime
 * doesn't keep paying full tick cost for them.
 */
@Component
public class GameClock {

    private static final long TICK_MS = 16;
    static final long BROADCAST_INTERVAL_MS = 50; // ~20fps

    private final RoomRegistry roomRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    private final Map<String, Long> lastBroadcastAtMillis = new ConcurrentHashMap<>();
    private final Set<String> finishedRooms = ConcurrentHashMap.newKeySet();

    @Autowired
    public GameClock(RoomRegistry roomRegistry, SimpMessagingTemplate messagingTemplate) {
        this(roomRegistry, messagingTemplate, Clock.systemUTC());
    }

    /** Package-private: lets tests inject a controllable clock for the broadcast-throttle behavior. */
    GameClock(RoomRegistry roomRegistry, SimpMessagingTemplate messagingTemplate, Clock clock) {
        this.roomRegistry = roomRegistry;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedRate = TICK_MS)
    public void tick() {
        long now = clock.millis();
        for (GameRoom room : roomRegistry.activeRooms()) {
            String roomId = room.roomId();
            if (finishedRooms.contains(roomId)) {
                continue; // already broadcast its final state -- nothing left to advance or send
            }

            GameEngine engine = room.engine();
            GameSnapshot snapshot;
            synchronized (engine) {
                room.publisher().advance(TICK_MS);
                snapshot = engine.snapshot(null);
            }

            boolean isFinal = snapshot.gameOver();
            Long lastBroadcastAt = lastBroadcastAtMillis.get(roomId);
            boolean dueForBroadcast = isFinal || lastBroadcastAt == null
                    || now - lastBroadcastAt >= BROADCAST_INTERVAL_MS;
            if (dueForBroadcast) {
                messagingTemplate.convertAndSend("/topic/game/" + roomId, snapshot);
                lastBroadcastAtMillis.put(roomId, now);
            }
            if (isFinal) {
                finishedRooms.add(roomId);
            }
        }
    }
}
