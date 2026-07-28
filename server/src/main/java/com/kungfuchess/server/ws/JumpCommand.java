package com.kungfuchess.server.ws;

/** Wire command for a jump attempt, scoped to one room. See {@link MoveCommand} for why this uses flat ints. */
public record JumpCommand(String roomId, int row, int col) {
}
