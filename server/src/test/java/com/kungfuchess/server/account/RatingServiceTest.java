package com.kungfuchess.server.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.bus.GameEndedEvent;
import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.game.RoomRegistry;

class RatingServiceTest {

    private static final String ROOM_ID = "room-1";

    private final Map<String, Player> byUsername = new HashMap<>();

    private PlayerRepository fakeRepository() {
        PlayerRepository repository = mock(PlayerRepository.class);
        when(repository.findByUsername(any())).thenAnswer(invocation ->
                Optional.ofNullable(byUsername.get(invocation.getArgument(0))));
        when(repository.save(any())).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            byUsername.put(player.getUsername(), player);
            return player;
        });
        return repository;
    }

    private RoomRegistry newRoomRegistry() {
        return new RoomRegistry(new GameEventBus(event -> { }));
    }

    /** Joins {@code ROOM_ID} in call order -- the first call becomes WHITE, the second BLACK. */
    private Player registerPlayer(PlayerRepository repository, RoomRegistry roomRegistry, String username,
            int rating) {
        Player player = new Player(username, "hash");
        player.setRating(rating);
        repository.save(player);
        roomRegistry.joinOrCreate(ROOM_ID, "session-" + username, username);
        return player;
    }

    @Test
    void equalRatingsWinnerGainsAndLoserLosesTheSameAmount() {
        PlayerRepository repository = fakeRepository();
        RoomRegistry roomRegistry = newRoomRegistry();
        registerPlayer(repository, roomRegistry, "alice", 1200);
        registerPlayer(repository, roomRegistry, "bob", 1200);
        RatingService service = new RatingService(repository, roomRegistry);

        service.onGameEnded(new GameEndedEvent(ROOM_ID, PieceColor.WHITE));

        assertEquals(1216, byUsername.get("alice").getRating());
        assertEquals(1184, byUsername.get("bob").getRating());
    }

    @Test
    void underdogWinningGainsMoreThanTheEqualRatingCase() {
        PlayerRepository repository = fakeRepository();
        RoomRegistry roomRegistry = newRoomRegistry();
        registerPlayer(repository, roomRegistry, "alice", 1000);
        registerPlayer(repository, roomRegistry, "bob", 1400);
        RatingService service = new RatingService(repository, roomRegistry);

        service.onGameEnded(new GameEndedEvent(ROOM_ID, PieceColor.WHITE));

        // expectedWhite = 1 / (1 + 10^((1400-1000)/400)) = 1/11 ~= 0.0909; delta = round(32 * (1 - 0.0909)) = 29
        assertEquals(1000 + 29, byUsername.get("alice").getRating());
        assertEquals(1400 - 29, byUsername.get("bob").getRating());
    }

    @Test
    void doesNothingWhenASeatHasNoLoggedInAccount() {
        PlayerRepository repository = fakeRepository();
        RoomRegistry roomRegistry = newRoomRegistry();
        registerPlayer(repository, roomRegistry, "alice", 1200);
        // no black player joined
        RatingService service = new RatingService(repository, roomRegistry);

        service.onGameEnded(new GameEndedEvent(ROOM_ID, PieceColor.WHITE));

        assertEquals(1200, byUsername.get("alice").getRating());
    }

    @Test
    void doesNothingWhenTheRoomCannotBeFound() {
        PlayerRepository repository = fakeRepository();
        RoomRegistry roomRegistry = newRoomRegistry();
        RatingService service = new RatingService(repository, roomRegistry);

        service.onGameEnded(new GameEndedEvent("no-such-room", PieceColor.WHITE));

        // no exception, nothing to assert beyond "didn't throw"
    }
}
