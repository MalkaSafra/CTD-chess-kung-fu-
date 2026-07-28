package com.kungfuchess.server.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.account.Player;
import com.kungfuchess.server.account.PlayerRepository;
import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.game.MatchmakingQueue;
import com.kungfuchess.server.game.RoomRegistry;
import com.kungfuchess.server.game.SessionAccountRegistry;

class MatchmakingControllerTest {

    private final Map<String, Player> byUsername = new HashMap<>();

    private PlayerRepository fakeRepository() {
        PlayerRepository repository = mock(PlayerRepository.class);
        when(repository.findByUsername(any())).thenAnswer(invocation ->
                Optional.ofNullable(byUsername.get(invocation.getArgument(0))));
        return repository;
    }

    private Player registerPlayer(String username, int rating) {
        Player player = new Player(username, "hash");
        player.setRating(rating);
        byUsername.put(username, player);
        return player;
    }

    private RoomRegistry newRoomRegistry() {
        return new RoomRegistry(new GameEventBus(event -> { }));
    }

    @Test
    void aLoneSearcherGetsNoNotificationYet() {
        registerPlayer("alice", 1200);
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "alice");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        MatchmakingController controller = new MatchmakingController(new MatchmakingQueue(), sessionAccountRegistry,
                fakeRepository(), newRoomRegistry(), messagingTemplate);

        controller.handlePlay("s1");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void twoCompatiblePlayersAreMatchedAndBothNotifiedWithOppositeColorsInAFreshRoom() {
        registerPlayer("alice", 1200);
        registerPlayer("bob", 1250);
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "alice");
        sessionAccountRegistry.recordLogin("s2", "bob");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomRegistry roomRegistry = newRoomRegistry();
        MatchmakingController controller = new MatchmakingController(new MatchmakingQueue(), sessionAccountRegistry,
                fakeRepository(), roomRegistry, messagingTemplate);

        controller.handlePlay("s1");
        controller.handlePlay("s2");

        String roomId = roomRegistry.roomForSession("s1").roomId();
        assertEquals(PieceColor.WHITE, roomRegistry.findById(roomId).colorOf("s1"));
        assertEquals(PieceColor.BLACK, roomRegistry.findById(roomId).colorOf("s2"));

        verify(messagingTemplate).convertAndSendToUser(eq("s1"), eq("/queue/match"),
                eq(MatchResult.matched(roomId, PieceColor.WHITE, "bob", 1250)), any(MessageHeaders.class));
        verify(messagingTemplate).convertAndSendToUser(eq("s2"), eq("/queue/match"),
                eq(MatchResult.matched(roomId, PieceColor.BLACK, "alice", 1200)), any(MessageHeaders.class));
    }

    @Test
    void playRequestBeforeLoginIsIgnored() {
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        MatchmakingController controller = new MatchmakingController(new MatchmakingQueue(), sessionAccountRegistry,
                fakeRepository(), newRoomRegistry(), messagingTemplate);

        controller.handlePlay("never-logged-in");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void concurrentMatchesGetSeparateIsolatedRooms() {
        registerPlayer("alice", 1200);
        registerPlayer("bob", 1200);
        registerPlayer("carol", 1200);
        registerPlayer("dave", 1200);
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "alice");
        sessionAccountRegistry.recordLogin("s2", "bob");
        sessionAccountRegistry.recordLogin("s3", "carol");
        sessionAccountRegistry.recordLogin("s4", "dave");
        RoomRegistry roomRegistry = newRoomRegistry();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        MatchmakingController controller = new MatchmakingController(new MatchmakingQueue(), sessionAccountRegistry,
                fakeRepository(), roomRegistry, messagingTemplate);

        controller.handlePlay("s1");
        controller.handlePlay("s2"); // pairs with s1
        controller.handlePlay("s3");
        controller.handlePlay("s4"); // pairs with s3, in a separate room, at the same time as s1/s2's game

        String firstRoomId = roomRegistry.roomForSession("s1").roomId();
        String secondRoomId = roomRegistry.roomForSession("s3").roomId();
        assertNotEquals(firstRoomId, secondRoomId);
    }

    @Test
    void unknownAccountFallsBackToStartingRatingRatherThanFailing() {
        // Session logged in, but its account row somehow isn't in the repository (defensive case).
        SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
        sessionAccountRegistry.recordLogin("s1", "ghost");
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomRegistry roomRegistry = newRoomRegistry();
        MatchmakingController controller = new MatchmakingController(new MatchmakingQueue(), sessionAccountRegistry,
                fakeRepository(), roomRegistry, messagingTemplate);

        controller.handlePlay("s1");

        assertNull(roomRegistry.roomForSession("s1"), "a lone searcher gets no room yet");
        verifyNoInteractions(messagingTemplate);
    }
}
