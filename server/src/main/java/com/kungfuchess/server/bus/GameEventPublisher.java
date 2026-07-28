package com.kungfuchess.server.bus;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.model.PieceColor;

/**
 * Bridges one room's live {@link GameEngine} to the {@link GameEventBus}. {@link MoveLoggedEvent}
 * is wired through the engine's existing {@code addMoveListener} hook, since that already fires
 * synchronously the moment a move/jump is recorded. Score and game-end have no equivalent hook, so
 * {@link #advance} diffs a {@link GameSnapshot} taken before and after each tick -- {@code
 * GameSnapshot} already carries {@code whiteScore}/{@code blackScore}/{@code gameOver}/{@code
 * winner}, built for rendering but just as usable here as a read-only state check. No engine
 * changes were needed for any of this.
 *
 * <p>{@code roomId} is stamped onto {@link GameEndedEvent} so a subscriber (e.g. {@code
 * RatingService}) can resolve which room's players to credit, now that a server can have several
 * of these publishers alive at once, one per {@code GameRoom}.
 */
public class GameEventPublisher {

    private final GameEngine engine;
    private final GameEventBus bus;
    private final String roomId;
    private boolean started;

    public GameEventPublisher(GameEngine engine, GameEventBus bus, String roomId) {
        this.engine = engine;
        this.bus = bus;
        this.roomId = roomId;
        engine.addMoveListener(record -> bus.publish(new MoveLoggedEvent(record)));
    }

    /** Advances the game clock by {@code ms}, publishing any score/game-end events it causes. */
    public void advance(long ms) {
        if (!started) {
            started = true;
            bus.publish(new GameStartedEvent());
        }

        GameSnapshot before = engine.snapshot(null);
        engine.waitClock(ms);
        GameSnapshot after = engine.snapshot(null);

        publishScoreChange(PieceColor.WHITE, before.whiteScore(), after.whiteScore());
        publishScoreChange(PieceColor.BLACK, before.blackScore(), after.blackScore());

        if (!before.gameOver() && after.gameOver()) {
            bus.publish(new GameEndedEvent(roomId, after.winner()));
        }
    }

    private void publishScoreChange(PieceColor color, int before, int after) {
        if (before != after) {
            bus.publish(new ScoreUpdatedEvent(color, after));
        }
    }
}
