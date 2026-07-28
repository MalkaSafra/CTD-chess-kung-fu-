package com.kungfuchess.server.ws;

import com.kungfuchess.model.PieceColor;

/**
 * The outcome of a matchmaking search, pushed to {@code /user/queue/match} once available -- not
 * a direct reply, since a match is only known once a *second* session's {@code /app/play} pairs
 * with this one, and a search that times out has no triggering incoming message to reply to
 * either (see {@code MatchmakingScheduler}). {@code roomId} is the fresh, isolated room {@code
 * RoomRegistry} created for this pairing -- the client subscribes to that room's own {@code
 * /topic/game/{roomId}} and sends its moves tagged with it.
 */
public record MatchResult(boolean matched, String roomId, PieceColor color, String opponentUsername,
        int opponentRating, String failureReason) {

    public static MatchResult matched(String roomId, PieceColor color, String opponentUsername, int opponentRating) {
        return new MatchResult(true, roomId, color, opponentUsername, opponentRating, null);
    }

    public static MatchResult failed(String reason) {
        return new MatchResult(false, null, null, null, 0, reason);
    }
}
