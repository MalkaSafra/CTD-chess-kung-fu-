package com.kungfuchess.server.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.account.Player;
import com.kungfuchess.server.account.PlayerRepository;
import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.game.DisconnectResignHandler;
import com.kungfuchess.server.game.RoomRegistry;
import com.kungfuchess.server.game.SessionAccountRegistry;

class LoginControllerTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SessionAccountRegistry sessionAccountRegistry = new SessionAccountRegistry();
    private final RoomRegistry roomRegistry = new RoomRegistry(new GameEventBus(event -> { }));
    private final DisconnectResignHandler disconnectResignHandler = new DisconnectResignHandler(roomRegistry);

    /** A repository that behaves like a real one backed by an in-memory map, via a mock. */
    private PlayerRepository fakeRepository() {
        PlayerRepository repository = mock(PlayerRepository.class);
        java.util.Map<String, Player> byUsername = new java.util.HashMap<>();
        when(repository.findByUsername(any())).thenAnswer(invocation ->
                Optional.ofNullable(byUsername.get(invocation.getArgument(0))));
        when(repository.save(any())).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            byUsername.put(player.getUsername(), player);
            return player;
        });
        return repository;
    }

    private LoginController newController(PlayerRepository repository) {
        return new LoginController(sessionAccountRegistry, repository, passwordEncoder, disconnectResignHandler,
                roomRegistry);
    }

    @Test
    void firstLoginForAUsernameAutoRegistersTheAccount() {
        PlayerRepository repository = fakeRepository();
        LoginController controller = newController(repository);

        LoginResponse response = controller.handleLogin(new LoginCommand("alice", "secret"), "session-1");

        assertTrue(response.success());
        assertEquals(Player.STARTING_RATING, response.rating());
        assertNull(response.reclaimedSeat(), "an ordinary login has no seat to reclaim");
        assertTrue(repository.findByUsername("alice").isPresent());
    }

    @Test
    void repeatLoginWithCorrectPasswordSucceeds() {
        PlayerRepository repository = fakeRepository();
        LoginController controller = newController(repository);
        controller.handleLogin(new LoginCommand("alice", "secret"), "session-1");

        LoginResponse response = controller.handleLogin(new LoginCommand("alice", "secret"), "session-1");

        assertTrue(response.success());
    }

    @Test
    void repeatLoginWithWrongPasswordFails() {
        PlayerRepository repository = fakeRepository();
        LoginController controller = newController(repository);
        controller.handleLogin(new LoginCommand("alice", "secret"), "session-1");

        LoginResponse response = controller.handleLogin(new LoginCommand("alice", "WRONG"), "session-2");

        assertFalse(response.success());
        assertEquals("Incorrect password", response.errorMessage());
    }

    @Test
    void reconnectingWithinTheGracePeriodReturnsTheReclaimedSeatWithOpponentInfo() {
        PlayerRepository repository = fakeRepository();
        LoginController controller = newController(repository);
        controller.handleLogin(new LoginCommand("alice", "secret"), "old-session");
        controller.handleLogin(new LoginCommand("bob", "hunter2"), "bob-session");
        roomRegistry.joinOrCreate("room-1", "old-session", "alice");
        roomRegistry.joinOrCreate("room-1", "bob-session", "bob");

        disconnectResignHandler.onSessionDisconnect(disconnectEvent("old-session"));
        LoginResponse response = controller.handleLogin(new LoginCommand("alice", "secret"), "new-session");

        assertTrue(response.success());
        MatchResult reclaimed = response.reclaimedSeat();
        assertEquals("room-1", reclaimed.roomId());
        assertEquals(PieceColor.WHITE, reclaimed.color());
        assertEquals("bob", reclaimed.opponentUsername());
        assertEquals(Player.STARTING_RATING, reclaimed.opponentRating());
        assertEquals(PieceColor.WHITE, roomRegistry.findById("room-1").colorOf("new-session"));
        assertNull(roomRegistry.findById("room-1").colorOf("old-session"));
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }
}
