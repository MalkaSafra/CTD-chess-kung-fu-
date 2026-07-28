package com.kungfuchess.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class MatchmakingQueueTest {

    /** A {@link Clock} the test can move forward on demand, to exercise the 60s-timeout path. */
    private static final class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(long seconds) {
            now = now.plusSeconds(seconds);
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
        public Clock withZone(java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void aLoneSessionDoesNotMatch() {
        MatchmakingQueue queue = new MatchmakingQueue(new ManualClock());

        assertTrue(queue.enqueue("s1", "alice", 1200).isEmpty());
        assertEquals(1, queue.waitingCount());
    }

    @Test
    void twoSessionsWithinRatingRangeMatchImmediately() {
        MatchmakingQueue queue = new MatchmakingQueue(new ManualClock());
        queue.enqueue("s1", "alice", 1200);

        Match match = queue.enqueue("s2", "bob", 1290).orElseThrow();

        assertEquals("alice", match.first().username());
        assertEquals("bob", match.second().username());
        assertEquals(0, queue.waitingCount());
    }

    @Test
    void twoSessionsOutsideRatingRangeDoNotMatch() {
        MatchmakingQueue queue = new MatchmakingQueue(new ManualClock());
        queue.enqueue("s1", "alice", 1000);

        assertTrue(queue.enqueue("s2", "bob", 1500).isEmpty());
        assertEquals(2, queue.waitingCount());
    }

    @Test
    void ratingConstraintNeverRelaxesEvenAfterTheTimeoutWindow() {
        ManualClock clock = new ManualClock();
        MatchmakingQueue queue = new MatchmakingQueue(clock);
        queue.enqueue("s1", "alice", 1000);
        clock.advance(90);

        assertTrue(queue.enqueue("s2", "bob", 1500).isEmpty());
        assertTrue(queue.tryMatch().isEmpty());
    }

    @Test
    void expireStaleWaitersRemovesSessionsWaitingAtLeastSixtySeconds() {
        ManualClock clock = new ManualClock();
        MatchmakingQueue queue = new MatchmakingQueue(clock);
        queue.enqueue("s1", "alice", 1000);
        clock.advance(60);

        List<MatchCandidate> expired = queue.expireStaleWaiters();

        assertEquals(1, expired.size());
        assertEquals("alice", expired.get(0).username());
        assertEquals(0, queue.waitingCount());
    }

    @Test
    void expireStaleWaitersLeavesRecentSessionsInTheQueue() {
        ManualClock clock = new ManualClock();
        MatchmakingQueue queue = new MatchmakingQueue(clock);
        queue.enqueue("s1", "alice", 1000);
        clock.advance(59);

        List<MatchCandidate> expired = queue.expireStaleWaiters();

        assertTrue(expired.isEmpty());
        assertEquals(1, queue.waitingCount());
    }

    @Test
    void removeTakesASessionOutOfTheQueue() {
        MatchmakingQueue queue = new MatchmakingQueue(new ManualClock());
        queue.enqueue("s1", "alice", 1000);

        queue.remove("s1");

        assertEquals(0, queue.waitingCount());
        assertTrue(queue.enqueue("s2", "bob", 1500).isEmpty());
    }
}
