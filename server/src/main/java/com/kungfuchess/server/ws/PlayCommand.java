package com.kungfuchess.server.ws;

/** Enters the sender's session into matchmaking. No fields -- the session id (from the STOMP
 * frame) and the account (already known via {@code PlayerRegistry}) are all the server needs. */
public record PlayCommand() {
}
