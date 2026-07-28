package com.kungfuchess.server.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Pairs waiting sessions strictly by rating: only two candidates within {@link #RATING_RANGE} of
 * each other can match. There is no fallback that relaxes the range -- a session that's been
 * waiting {@link #TIMEOUT} without a match is instead expired out of the queue by {@link
 * #expireStaleWaiters()} so the caller can tell that client the search timed out, and the player
 * can choose to search again.
 */
@Component
public class MatchmakingQueue {

    static final int RATING_RANGE = 100;
    static final Duration TIMEOUT = Duration.ofSeconds(60);

    private record Waiting(String sessionId, String username, int rating, Instant enqueuedAt) {
    }

    private final List<Waiting> waiting = new ArrayList<>();
    private final Clock clock;

    public MatchmakingQueue() {
        this(Clock.systemUTC());
    }

    /** Package-private: lets tests inject a controllable clock for the 60s-timeout behavior. */
    MatchmakingQueue(Clock clock) {
        this.clock = clock;
    }

    /** Adds a session to the queue, then immediately checks whether it completes a match. */
    public synchronized Optional<Match> enqueue(String sessionId, String username, int rating) {
        waiting.add(new Waiting(sessionId, username, rating, clock.instant()));
        return tryMatch();
    }

    /** Re-scans the whole queue for any pair within {@link #RATING_RANGE} of each other. */
    public synchronized Optional<Match> tryMatch() {
        for (int i = 0; i < waiting.size(); i++) {
            for (int j = i + 1; j < waiting.size(); j++) {
                Waiting a = waiting.get(i);
                Waiting b = waiting.get(j);
                if (Math.abs(a.rating() - b.rating()) <= RATING_RANGE) {
                    waiting.remove(j);
                    waiting.remove(i);
                    return Optional.of(new Match(
                            new MatchCandidate(a.sessionId(), a.username(), a.rating()),
                            new MatchCandidate(b.sessionId(), b.username(), b.rating())));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Removes and returns every session that has been waiting {@link #TIMEOUT} or longer without
     * a match. Meant to be polled on a schedule so a search can be aborted even without another
     * player enqueuing to trigger the check.
     */
    public synchronized List<MatchCandidate> expireStaleWaiters() {
        Instant now = clock.instant();
        List<MatchCandidate> expired = new ArrayList<>();
        Iterator<Waiting> it = waiting.iterator();
        while (it.hasNext()) {
            Waiting w = it.next();
            if (!Duration.between(w.enqueuedAt(), now).minus(TIMEOUT).isNegative()) {
                expired.add(new MatchCandidate(w.sessionId(), w.username(), w.rating()));
                it.remove();
            }
        }
        return expired;
    }

    /** Takes a session out of the queue without matching it (e.g. it disconnected while waiting). */
    public synchronized void remove(String sessionId) {
        waiting.removeIf(w -> w.sessionId().equals(sessionId));
    }

    public synchronized int waitingCount() {
        return waiting.size();
    }
}
