package com.kungfuchess.server.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kungfuchess.server.game.MatchCandidate;
import com.kungfuchess.server.game.MatchmakingQueue;

/**
 * Polls {@link MatchmakingQueue} for searches that have hit the 60-second timeout with no match
 * found, and tells each such session so -- there's no incoming message to reply to for a timeout
 * (unlike a successful match, which is always discovered synchronously inside some session's
 * {@code /app/play}), so this has to run on its own schedule instead.
 */
@Component
public class MatchmakingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingScheduler.class);
    private static final long CHECK_INTERVAL_MS = 1000;

    private final MatchmakingQueue matchmakingQueue;
    private final SimpMessagingTemplate messagingTemplate;

    public MatchmakingScheduler(MatchmakingQueue matchmakingQueue, SimpMessagingTemplate messagingTemplate) {
        this.matchmakingQueue = matchmakingQueue;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void expireStaleSearches() {
        for (MatchCandidate expired : matchmakingQueue.expireStaleWaiters()) {
            log.info("Matchmaking search for '{}' timed out with no match found", expired.username());
            UserSessionMessaging.sendToSession(messagingTemplate, expired.sessionId(), "/queue/match",
                    MatchResult.failed("No match found within 60 seconds."));
        }
    }
}
