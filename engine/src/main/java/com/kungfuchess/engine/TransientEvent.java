package com.kungfuchess.engine;

/**
 * A momentary, global thing that happened during the tick a {@link GameSnapshot} was built for --
 * e.g. so a client can play a sound without having to infer it by diffing consecutive snapshots.
 * Deliberately restricted to events every observer should see identically: something like a move
 * rejection is per-requester, not global, and must never go here -- see
 * {@code com.kungfuchess.server.ws.GameMessageController} for how that's handled instead (a
 * private reply to the requesting session, not a broadcast field).
 */
public enum TransientEvent {
    PAWN_PROMOTED
}
