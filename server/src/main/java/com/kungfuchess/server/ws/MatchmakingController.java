package com.kungfuchess.server.ws;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.account.Player;
import com.kungfuchess.server.account.PlayerRepository;
import com.kungfuchess.server.game.GameRoom;
import com.kungfuchess.server.game.Match;
import com.kungfuchess.server.game.MatchCandidate;
import com.kungfuchess.server.game.MatchmakingQueue;
import com.kungfuchess.server.game.RoomRegistry;
import com.kungfuchess.server.game.SessionAccountRegistry;

/**
 * Handles {@code /app/play}: enqueues the requesting session into {@link MatchmakingQueue} using
 * its account's current rating, and if that enqueue immediately completes a pairing, spins up a
 * fresh, isolated {@link GameRoom} for the two of them (via {@link RoomRegistry#createRoomForMatch})
 * and notifies both sessions. A session that only completes a *later* pairing (found by {@code
 * MatchmakingScheduler}'s periodic {@code tryMatch} -- not needed today since every match is found
 * synchronously on the second player's enqueue, but the queue API supports it) would be notified
 * from there instead.
 *
 * <p>Unlike Stage 5, there's no "a game is already in progress" gate here anymore -- every match
 * gets its own room, so any number of games can run at once.
 */
@Controller
public class MatchmakingController {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingController.class);

    private final MatchmakingQueue matchmakingQueue;
    private final SessionAccountRegistry sessionAccountRegistry;
    private final PlayerRepository playerRepository;
    private final RoomRegistry roomRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public MatchmakingController(MatchmakingQueue matchmakingQueue, SessionAccountRegistry sessionAccountRegistry,
            PlayerRepository playerRepository, RoomRegistry roomRegistry, SimpMessagingTemplate messagingTemplate) {
        this.matchmakingQueue = matchmakingQueue;
        this.sessionAccountRegistry = sessionAccountRegistry;
        this.playerRepository = playerRepository;
        this.roomRegistry = roomRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/play")
    public void handlePlay(@Header("simpSessionId") String sessionId) {
        String username = sessionAccountRegistry.usernameOf(sessionId);
        if (username == null) {
            log.warn("Session {} requested matchmaking before logging in", sessionId);
            return;
        }

        int rating = playerRepository.findByUsername(username).map(Player::getRating).orElse(Player.STARTING_RATING);
        log.info("Player '{}' (rating {}) is searching for a match", username, rating);

        Optional<Match> match = matchmakingQueue.enqueue(sessionId, username, rating);
        match.ifPresent(this::notifyMatch);
    }

    private void notifyMatch(Match match) {
        MatchCandidate white = match.first();
        MatchCandidate black = match.second();
        GameRoom room = roomRegistry.createRoomForMatch(white.sessionId(), white.username(), black.sessionId(),
                black.username());

        log.info("Matched '{}' (WHITE) vs '{}' (BLACK) in room '{}'", white.username(), black.username(),
                room.roomId());

        sendResult(white.sessionId(), MatchResult.matched(room.roomId(), PieceColor.WHITE, black.username(),
                black.rating()));
        sendResult(black.sessionId(), MatchResult.matched(room.roomId(), PieceColor.BLACK, white.username(),
                white.rating()));
    }

    private void sendResult(String sessionId, MatchResult result) {
        UserSessionMessaging.sendToSession(messagingTemplate, sessionId, "/queue/match", result);
    }
}
