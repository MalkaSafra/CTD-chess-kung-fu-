package com.kungfuchess.net;

import com.kungfuchess.model.PieceColor;

/** Mirrors {@code com.kungfuchess.server.ws.MatchResult}. See {@link MoveCommand} for why. */
public record MatchResult(boolean matched, String roomId, PieceColor color, String opponentUsername,
        int opponentRating, String failureReason) {
}
