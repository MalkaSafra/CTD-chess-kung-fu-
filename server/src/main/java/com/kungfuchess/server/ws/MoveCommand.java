package com.kungfuchess.server.ws;

/**
 * Wire command for a plain move/capture attempt, scoped to one room since a server can now run
 * several games at once. Flat row/col ints rather than a nested {@code Position} -- {@code
 * Position} isn't a record and has no default constructor, so it isn't Jackson-deserializable
 * without either annotating it (pulling a Jackson dependency into the framework-free {@code
 * engine} module) or compiling {@code engine} with {@code -parameters}. Flat ints sidestep the
 * question entirely for this internal, not-a-public-API wire format.
 */
public record MoveCommand(String roomId, int fromRow, int fromCol, int toRow, int toCol) {
}
