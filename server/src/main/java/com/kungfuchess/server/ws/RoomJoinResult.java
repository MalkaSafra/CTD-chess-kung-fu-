package com.kungfuchess.server.ws;

import com.kungfuchess.server.game.RoomRole;

/**
 * The outcome of a Create/Join request, pushed to {@code /user/queue/room}. {@code whiteUsername}/
 * {@code blackUsername} are whoever currently holds those seats in the room (possibly {@code null}
 * if a seat is still open) -- a spectator needs both to know who it's watching, and a player
 * joining second needs the first player's name the same way a matched opponent's name is reported
 * by {@code MatchResult}.
 */
public record RoomJoinResult(boolean success, String roomId, RoomRole role, String whiteUsername,
        String blackUsername, String failureReason) {

    public static RoomJoinResult joined(String roomId, RoomRole role, String whiteUsername, String blackUsername) {
        return new RoomJoinResult(true, roomId, role, whiteUsername, blackUsername, null);
    }

    public static RoomJoinResult failed(String reason) {
        return new RoomJoinResult(false, null, null, null, null, reason);
    }
}
