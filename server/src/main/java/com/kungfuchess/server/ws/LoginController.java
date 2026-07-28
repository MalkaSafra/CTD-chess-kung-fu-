package com.kungfuchess.server.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import com.kungfuchess.server.account.Player;
import com.kungfuchess.server.account.PlayerRepository;
import com.kungfuchess.server.game.DisconnectResignHandler;
import com.kungfuchess.server.game.DisconnectResignHandler.ReclaimedSeat;
import com.kungfuchess.server.game.GameRoom;
import com.kungfuchess.server.game.RoomRegistry;
import com.kungfuchess.server.game.SessionAccountRegistry;

/**
 * Authenticates against {@link PlayerRepository}, auto-registering a new account on a username's
 * first login (there's no separate "sign up" flow in the deck). No seat is granted here in the
 * ordinary case -- that only happens once matchmaking (see {@code MatchmakingController}) or the
 * Create/Join dialog (see {@code RoomController}) puts this session into a room. The one exception
 * is a reconnect within {@link DisconnectResignHandler}'s 20-second grace period, handled here
 * since login is exactly the moment the server learns "this account is back."
 */
@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final SessionAccountRegistry sessionAccountRegistry;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final DisconnectResignHandler disconnectResignHandler;
    private final RoomRegistry roomRegistry;

    public LoginController(SessionAccountRegistry sessionAccountRegistry, PlayerRepository playerRepository,
            PasswordEncoder passwordEncoder, DisconnectResignHandler disconnectResignHandler,
            RoomRegistry roomRegistry) {
        this.sessionAccountRegistry = sessionAccountRegistry;
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.disconnectResignHandler = disconnectResignHandler;
        this.roomRegistry = roomRegistry;
    }

    @MessageMapping("/login")
    @SendToUser("/queue/login")
    public LoginResponse handleLogin(LoginCommand command, @Header("simpSessionId") String sessionId) {
        String username = command.username();

        Player player = playerRepository.findByUsername(username).orElse(null);
        if (player == null) {
            player = new Player(username, passwordEncoder.encode(command.password()));
            playerRepository.save(player);
            log.info("Registered new account '{}'", username);
        } else if (!passwordEncoder.matches(command.password(), player.getPasswordHash())) {
            log.info("Player '{}' failed login: incorrect password", username);
            return LoginResponse.failure("Incorrect password");
        }

        sessionAccountRegistry.recordLogin(sessionId, username);

        ReclaimedSeat reclaimed = disconnectResignHandler.tryReclaimSeat(username, sessionId);
        if (reclaimed != null) {
            return LoginResponse.successWithReclaimedSeat(player.getRating(), reclaimedMatchResult(reclaimed));
        }

        log.info("Player '{}' logged in (rating {})", username, player.getRating());
        return LoginResponse.success(player.getRating());
    }

    private MatchResult reclaimedMatchResult(ReclaimedSeat reclaimed) {
        GameRoom room = roomRegistry.findById(reclaimed.roomId());
        String opponentUsername = room.usernameForColor(reclaimed.color().opposite());
        int opponentRating = playerRepository.findByUsername(opponentUsername)
                .map(Player::getRating)
                .orElse(Player.STARTING_RATING);
        return MatchResult.matched(reclaimed.roomId(), reclaimed.color(), opponentUsername, opponentRating);
    }
}
