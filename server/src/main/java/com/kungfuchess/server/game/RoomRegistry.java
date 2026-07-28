package com.kungfuchess.server.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.io.StandardBoard;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.RuleEngine;
import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.bus.GameEventPublisher;

/**
 * Owns every concurrently-running {@link GameRoom}, replacing the single shared {@code GameEngine}
 * bean the server used to have. Rooms come from two sources: explicitly, by room code, via {@link
 * #joinOrCreate} (the client's Create/Join dialog -- entering a new code creates the room, entering
 * an existing one joins it), or implicitly, one fresh isolated room per successful matchmaking
 * pairing, via {@link #createRoomForMatch}.
 */
@Component
public class RoomRegistry {

    private final GameEventBus eventBus;
    private final Map<String, GameRoom> roomsById = new ConcurrentHashMap<>();
    private final Map<String, String> roomIdBySession = new ConcurrentHashMap<>();
    private final AtomicInteger matchRoomSequence = new AtomicInteger();

    public RoomRegistry(GameEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Joins the room with this code, creating a fresh one first if no such code exists yet. The
     * first two distinct sessions to join become WHITE/BLACK; anyone after that joins as a
     * spectator (see {@link GameRoom#join}).
     */
    public RoomRole joinOrCreate(String roomCode, String sessionId, String username) {
        GameRoom room = roomsById.computeIfAbsent(roomCode, this::newRoom);
        roomIdBySession.put(sessionId, roomCode);
        return room.join(sessionId, username);
    }

    /** Creates a fresh, isolated room for a matchmaking pairing and seats both sessions immediately. */
    public GameRoom createRoomForMatch(String whiteSessionId, String whiteUsername, String blackSessionId,
            String blackUsername) {
        String roomId = "match-" + matchRoomSequence.incrementAndGet();
        GameRoom room = newRoom(roomId);
        roomsById.put(roomId, room);
        room.assignSeat(whiteSessionId, PieceColor.WHITE, whiteUsername);
        room.assignSeat(blackSessionId, PieceColor.BLACK, blackUsername);
        roomIdBySession.put(whiteSessionId, roomId);
        roomIdBySession.put(blackSessionId, roomId);
        return room;
    }

    public GameRoom findById(String roomId) {
        return roomsById.get(roomId);
    }

    /** The room a session is currently a player or spectator in, or {@code null}. */
    public GameRoom roomForSession(String sessionId) {
        String roomId = roomIdBySession.get(sessionId);
        return roomId == null ? null : roomsById.get(roomId);
    }

    /** Drops the session-to-room mapping (e.g. a spectator disconnecting for good). */
    public void forgetSession(String sessionId) {
        roomIdBySession.remove(sessionId);
    }

    /** Moves the room mapping from a disconnected session to the one that reclaimed its seat. */
    public void moveSession(String oldSessionId, String newSessionId) {
        String roomId = roomIdBySession.remove(oldSessionId);
        if (roomId != null) {
            roomIdBySession.put(newSessionId, roomId);
        }
    }

    /** Every room currently live, for {@code GameClock} to tick each one every frame. */
    public Iterable<GameRoom> activeRooms() {
        return roomsById.values();
    }

    private GameRoom newRoom(String roomId) {
        GameEngine engine = new GameEngine(new GameState(StandardBoard.create()), new RuleEngine(),
                new RealTimeArbiter());
        GameEventPublisher publisher = new GameEventPublisher(engine, eventBus, roomId);
        return new GameRoom(roomId, engine, publisher);
    }
}
