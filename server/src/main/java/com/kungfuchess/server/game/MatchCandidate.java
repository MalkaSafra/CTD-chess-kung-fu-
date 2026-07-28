package com.kungfuchess.server.game;

/** A session waiting for (or matched into) a game. */
public record MatchCandidate(String sessionId, String username, int rating) {
}
