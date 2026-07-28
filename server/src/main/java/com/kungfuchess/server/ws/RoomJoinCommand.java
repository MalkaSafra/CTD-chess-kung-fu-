package com.kungfuchess.server.ws;

/**
 * Wire command for the Create/Join dialog: entering a room code that doesn't exist yet creates
 * it, entering one that does joins it -- see {@code RoomController} for the full rule.
 */
public record RoomJoinCommand(String roomCode) {
}
