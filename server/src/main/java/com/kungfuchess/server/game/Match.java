package com.kungfuchess.server.game;

/** A pairing found by {@link MatchmakingQueue}. Which of the two becomes WHITE/BLACK is up to the caller. */
public record Match(MatchCandidate first, MatchCandidate second) {
}
