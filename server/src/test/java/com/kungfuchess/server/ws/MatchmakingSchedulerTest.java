package com.kungfuchess.server.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.kungfuchess.server.game.MatchCandidate;
import com.kungfuchess.server.game.MatchmakingQueue;

class MatchmakingSchedulerTest {

    @Test
    void aFreshSearchIsNotYetTimedOut() {
        MatchmakingQueue queue = new MatchmakingQueue();
        queue.enqueue("s1", "alice", 1200);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        MatchmakingScheduler scheduler = new MatchmakingScheduler(queue, messagingTemplate);

        scheduler.expireStaleSearches();

        verifyNoInteractions(messagingTemplate);
        assertEquals(1, queue.waitingCount());
    }

    @Test
    void aTimedOutSearchIsNotifiedWithAFailureResult() {
        // The 60s-timeout math itself is covered by MatchmakingQueueTest with a controllable
        // clock; this test only needs to prove the scheduler notifies whatever the queue reports
        // as expired, so a stub queue that always reports one expired waiter is enough here.
        MatchmakingQueue queue = new AlreadyExpiredQueue();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        MatchmakingScheduler scheduler = new MatchmakingScheduler(queue, messagingTemplate);

        scheduler.expireStaleSearches();

        verify(messagingTemplate).convertAndSendToUser(eq("s1"), eq("/queue/match"),
                eq(MatchResult.failed("No match found within 60 seconds.")), any(MessageHeaders.class));
    }

    private static final class AlreadyExpiredQueue extends MatchmakingQueue {
        @Override
        public synchronized List<MatchCandidate> expireStaleWaiters() {
            return List.of(new MatchCandidate("s1", "alice", 1200));
        }
    }
}
