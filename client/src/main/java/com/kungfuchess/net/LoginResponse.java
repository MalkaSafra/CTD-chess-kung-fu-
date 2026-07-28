package com.kungfuchess.net;

/** Mirrors {@code com.kungfuchess.server.ws.LoginResponse}. See {@link MoveCommand} for why. */
public record LoginResponse(boolean success, int rating, String errorMessage, MatchResult reclaimedSeat) {
}
