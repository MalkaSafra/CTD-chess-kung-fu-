package com.kungfuchess.server.game;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.bus.GameEventPublisher;

/**
 * One isolated game: its own {@link GameEngine}/{@link GameEventPublisher}, plus this room's own
 * two-seat player bookkeeping and spectator set. Every concurrent game gets its own instance of
 * this class, created by {@link RoomRegistry} -- this is what replaces the old single shared
 * {@code GameEngine} bean.
 */
public final class GameRoom {

    private record PlayerSeat(PieceColor color, String username) {
    }

    private final String roomId;
    private final GameEngine engine;
    private final GameEventPublisher publisher;

    private final Map<String, PlayerSeat> seatsBySession = new LinkedHashMap<>();
    private final Set<String> spectatorSessions = new LinkedHashSet<>();

    public GameRoom(String roomId, GameEngine engine, GameEventPublisher publisher) {
        this.roomId = roomId;
        this.engine = engine;
        this.publisher = publisher;
    }

    public String roomId() {
        return roomId;
    }

    public GameEngine engine() {
        return engine;
    }

    public GameEventPublisher publisher() {
        return publisher;
    }

    /**
     * Joins this room: fills an open seat (WHITE, then BLACK) if either is free, otherwise joins
     * as a read-only spectator. Idempotent for a session that already holds a seat or is already
     * spectating -- calling again just reports the same role back rather than granting a second one.
     */
    public synchronized RoomRole join(String sessionId, String username) {
        PlayerSeat existing = seatsBySession.get(sessionId);
        if (existing != null) {
            return RoomRole.forColor(existing.color());
        }
        if (spectatorSessions.contains(sessionId)) {
            return RoomRole.SPECTATOR;
        }

        PieceColor openSeat = nextOpenSeat();
        if (openSeat == null) {
            spectatorSessions.add(sessionId);
            return RoomRole.SPECTATOR;
        }
        seatsBySession.put(sessionId, new PlayerSeat(openSeat, username));
        return RoomRole.forColor(openSeat);
    }

    /** Directly grants a seat -- used by matchmaking, which has already decided both colors. */
    public synchronized void assignSeat(String sessionId, PieceColor color, String username) {
        seatsBySession.put(sessionId, new PlayerSeat(color, username));
    }

    /** Moves a disconnected player's seat to the session that reconnected in time. */
    public synchronized void reassignSeat(String oldSessionId, String newSessionId) {
        PlayerSeat seat = seatsBySession.remove(oldSessionId);
        if (seat != null) {
            seatsBySession.put(newSessionId, seat);
        }
    }

    public synchronized void removeSpectator(String sessionId) {
        spectatorSessions.remove(sessionId);
    }

    public synchronized boolean hasOpenSeats() {
        return seatsBySession.size() < 2;
    }

    public synchronized PieceColor colorOf(String sessionId) {
        PlayerSeat seat = seatsBySession.get(sessionId);
        return seat == null ? null : seat.color();
    }

    /** {@code null} if nobody currently holds that seat. */
    public synchronized String usernameForColor(PieceColor color) {
        for (PlayerSeat seat : seatsBySession.values()) {
            if (seat.color() == color) {
                return seat.username();
            }
        }
        return null;
    }

    private PieceColor nextOpenSeat() {
        boolean whiteTaken = false;
        boolean blackTaken = false;
        for (PlayerSeat seat : seatsBySession.values()) {
            if (seat.color() == PieceColor.WHITE) {
                whiteTaken = true;
            } else {
                blackTaken = true;
            }
        }
        if (!whiteTaken) {
            return PieceColor.WHITE;
        }
        if (!blackTaken) {
            return PieceColor.BLACK;
        }
        return null;
    }
}
