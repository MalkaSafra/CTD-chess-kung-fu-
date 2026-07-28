package com.kungfuchess.net;

/** Mirrors {@code com.kungfuchess.server.ws.RoomJoinResult}. See {@link MoveCommand} for why. */
public record RoomJoinResult(boolean success, String roomId, RoomRole role, String whiteUsername,
        String blackUsername, String failureReason) {
}
