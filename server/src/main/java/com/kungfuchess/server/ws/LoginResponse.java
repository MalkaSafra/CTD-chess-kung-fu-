package com.kungfuchess.server.ws;

/**
 * {@code success = false} means the username exists and the password didn't match -- the session
 * is not logged in at all and the client should re-prompt, not fall back to spectating. There is
 * no seat/color here in the ordinary case -- logging in only authenticates; a seat is granted
 * later, once matchmaking pairs this session with an opponent (see {@code MatchResult}).
 * {@code reclaimedSeat} is the one exception: if this login reconnects an account within its
 * 20-second disconnect grace period (see {@code DisconnectResignHandler}), its seat is restored
 * immediately, described the same way a fresh match would be. {@code rating} is only meaningful
 * when {@code success} is {@code true}.
 */
public record LoginResponse(boolean success, int rating, String errorMessage, MatchResult reclaimedSeat) {

    public static LoginResponse success(int rating) {
        return new LoginResponse(true, rating, null, null);
    }

    public static LoginResponse successWithReclaimedSeat(int rating, MatchResult reclaimedSeat) {
        return new LoginResponse(true, rating, null, reclaimedSeat);
    }

    public static LoginResponse failure(String errorMessage) {
        return new LoginResponse(false, 0, errorMessage, null);
    }
}
