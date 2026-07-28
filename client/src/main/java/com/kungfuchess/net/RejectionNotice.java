package com.kungfuchess.net;

/** Mirrors {@code com.kungfuchess.server.ws.RejectionNotice}. See {@link MoveCommand} for why. */
public record RejectionNotice(String reason) {
}
