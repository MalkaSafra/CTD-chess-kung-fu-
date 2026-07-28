package com.kungfuchess.server.game;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.model.PieceColor;

/**
 * The deck's 20-second disconnect auto-resign: when a seated session drops mid-game, that seat has
 * {@link #GRACE_PERIOD_SECONDS} to reconnect (see {@link #tryReclaimSeat}) before {@link
 * GameEngine#resign} ends that room's game in the opponent's favor. Ending the game this way --
 * rather than publishing a game-ended event directly -- deliberately reuses the exact same path a
 * normal king-capture end takes: the next {@code GameClock} tick for that room observes {@code
 * gameOver} flip via its usual before/after snapshot diff, broadcasts it, and fires {@code
 * GameEndedEvent} so {@code RatingService} updates ELO exactly as it already does for any other
 * game end.
 *
 * <p>A disconnecting spectator (no seat in its room) is simply dropped from the room's spectator
 * set -- there's nothing to resign.
 */
@Component
public class DisconnectResignHandler {

    private static final Logger log = LoggerFactory.getLogger(DisconnectResignHandler.class);
    static final long GRACE_PERIOD_SECONDS = 20;

    /** What a session was holding at the moment it disconnected, so a reconnect can find it again. */
    private record Disconnected(String roomId, PieceColor color, String username) {
    }

    /** The outcome of a successful {@link #tryReclaimSeat}. */
    public record ReclaimedSeat(String roomId, PieceColor color) {
    }

    private final RoomRegistry roomRegistry;
    private final ScheduledExecutorService scheduler;

    private final Map<String, Disconnected> disconnectedBySession = new HashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingResigns = new HashMap<>();

    @Autowired
    public DisconnectResignHandler(RoomRegistry roomRegistry) {
        this(roomRegistry, Executors.newSingleThreadScheduledExecutor(DisconnectResignHandler::newDaemonThread));
    }

    /** Package-private: lets tests inject a directly-controllable executor instead of waiting 20 real seconds. */
    DisconnectResignHandler(RoomRegistry roomRegistry, ScheduledExecutorService scheduler) {
        this.roomRegistry = roomRegistry;
        this.scheduler = scheduler;
    }

    private static Thread newDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "disconnect-resign-scheduler");
        thread.setDaemon(true);
        return thread;
    }

    @EventListener
    public synchronized void onSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        GameRoom room = roomRegistry.roomForSession(sessionId);
        if (room == null) {
            return; // never joined a room -- nothing to clean up or auto-resign
        }

        PieceColor color = room.colorOf(sessionId);
        if (color == null) {
            room.removeSpectator(sessionId);
            roomRegistry.forgetSession(sessionId);
            return;
        }

        GameEngine engine = room.engine();
        boolean gameOver;
        synchronized (engine) {
            gameOver = engine.snapshot(null).gameOver();
        }
        if (gameOver) {
            return;
        }

        log.info("Seated player ({}) disconnected from room '{}' -- {}s to reconnect before auto-resigning",
                color, room.roomId(), GRACE_PERIOD_SECONDS);
        disconnectedBySession.put(sessionId, new Disconnected(room.roomId(), color, room.usernameForColor(color)));
        pendingResigns.put(sessionId,
                scheduler.schedule(() -> resignIfStillPending(sessionId), GRACE_PERIOD_SECONDS, TimeUnit.SECONDS));
    }

    private synchronized void resignIfStillPending(String sessionId) {
        Disconnected info = disconnectedBySession.remove(sessionId);
        if (info == null) {
            return; // reconnected before the timer fired
        }
        pendingResigns.remove(sessionId);

        GameRoom room = roomRegistry.findById(info.roomId());
        if (room == null) {
            return;
        }
        log.info("{} did not reconnect within {}s in room '{}' -- auto-resigning", info.color(),
                GRACE_PERIOD_SECONDS, info.roomId());
        GameEngine engine = room.engine();
        synchronized (engine) {
            engine.resign(info.color());
        }
    }

    /**
     * Called from {@code LoginController} right after a successful login: if {@code username}
     * matches the account that just disconnected from a still-in-progress room, cancels the
     * pending auto-resign and moves that seat to {@code newSessionId}. Returns the reclaimed
     * room/color, or {@code null} if this was an ordinary login with no seat to reclaim.
     */
    public synchronized ReclaimedSeat tryReclaimSeat(String username, String newSessionId) {
        String oldSessionId = null;
        Disconnected info = null;
        for (Map.Entry<String, Disconnected> entry : disconnectedBySession.entrySet()) {
            if (username.equals(entry.getValue().username())) {
                oldSessionId = entry.getKey();
                info = entry.getValue();
                break;
            }
        }
        if (info == null) {
            return null;
        }

        disconnectedBySession.remove(oldSessionId);
        ScheduledFuture<?> future = pendingResigns.remove(oldSessionId);
        if (future != null) {
            future.cancel(false);
        }

        GameRoom room = roomRegistry.findById(info.roomId());
        if (room == null) {
            return null;
        }
        room.reassignSeat(oldSessionId, newSessionId);
        roomRegistry.moveSession(oldSessionId, newSessionId);
        log.info("Player '{}' reconnected in time, resuming as {} in room '{}'", username, info.color(),
                info.roomId());
        return new ReclaimedSeat(info.roomId(), info.color());
    }
}
