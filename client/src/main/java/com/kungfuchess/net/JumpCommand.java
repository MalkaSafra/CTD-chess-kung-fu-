package com.kungfuchess.net;

/** Mirrors {@code com.kungfuchess.server.ws.JumpCommand}. See {@link MoveCommand} for why. */
public record JumpCommand(String roomId, int row, int col) {
}
