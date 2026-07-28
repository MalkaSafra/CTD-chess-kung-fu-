package com.kungfuchess.server.account;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.bus.GameEndedEvent;
import com.kungfuchess.server.game.GameRoom;
import com.kungfuchess.server.game.RoomRegistry;

/**
 * Updates both players' ELO ratings when a game ends. Standard ELO, K=32. Does nothing if the
 * room can't be found, or either seat isn't held by a logged-in account (e.g. a headless/test game
 * with no real players) -- there's nobody to persist a rating for.
 */
@Component
public class RatingService {

    private static final int K_FACTOR = 32;

    private final PlayerRepository playerRepository;
    private final RoomRegistry roomRegistry;

    public RatingService(PlayerRepository playerRepository, RoomRegistry roomRegistry) {
        this.playerRepository = playerRepository;
        this.roomRegistry = roomRegistry;
    }

    @EventListener
    public void onGameEnded(GameEndedEvent event) {
        GameRoom room = roomRegistry.findById(event.roomId());
        if (room == null) {
            return;
        }

        String whiteUsername = room.usernameForColor(PieceColor.WHITE);
        String blackUsername = room.usernameForColor(PieceColor.BLACK);
        if (whiteUsername == null || blackUsername == null) {
            return;
        }

        Player white = playerRepository.findByUsername(whiteUsername).orElse(null);
        Player black = playerRepository.findByUsername(blackUsername).orElse(null);
        if (white == null || black == null) {
            return;
        }

        double whiteResult = event.winner() == PieceColor.WHITE ? 1.0 : 0.0;
        applyResult(white, black, whiteResult);

        playerRepository.save(white);
        playerRepository.save(black);
    }

    private void applyResult(Player white, Player black, double whiteResult) {
        double expectedWhite = expectedScore(white.getRating(), black.getRating());
        double expectedBlack = 1.0 - expectedWhite;
        double blackResult = 1.0 - whiteResult;

        white.setRating(white.getRating() + (int) Math.round(K_FACTOR * (whiteResult - expectedWhite)));
        black.setRating(black.getRating() + (int) Math.round(K_FACTOR * (blackResult - expectedBlack)));
    }

    private double expectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
    }
}
