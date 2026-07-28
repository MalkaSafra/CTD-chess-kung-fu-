package com.kungfuchess.net;

/**
 * Mirrors {@code com.kungfuchess.server.ws.MoveCommand} exactly. Deliberately duplicated rather
 * than shared: {@code client} and {@code server} have no dependency on each other (by design --
 * see the earlier discussion on why {@code input}/{@code io} stay in {@code engine} rather than a
 * fourth shared module), so the two sides of this wire contract are each free to evolve
 * independently, the same way two independently deployed services would.
 */
public record MoveCommand(String roomId, int fromRow, int fromCol, int toRow, int toCol) {
}
