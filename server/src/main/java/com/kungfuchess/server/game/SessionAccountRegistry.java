package com.kungfuchess.server.game;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Session-to-account bookkeeping: which username a session authenticated as. Truly global and
 * unaffected by rooms -- a session logs in once, then may join/create any number of rooms (see
 * {@link RoomRegistry}) as a player or spectator. This is the identity half of what used to be
 * the single-game {@code PlayerRegistry}; the seat/color half is now {@link GameRoom}'s own
 * instance state, since a session's color only makes sense within one specific room.
 */
@Component
public class SessionAccountRegistry {

    private final Map<String, String> usernamesBySession = new LinkedHashMap<>();

    /** Records which account this session authenticated as. Idempotent. */
    public synchronized void recordLogin(String sessionId, String username) {
        usernamesBySession.put(sessionId, username);
    }

    /** {@code null} if this session never logged in. */
    public synchronized String usernameOf(String sessionId) {
        return usernamesBySession.get(sessionId);
    }
}
