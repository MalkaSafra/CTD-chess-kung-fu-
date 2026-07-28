package com.kungfuchess.server.bus;

/** Published once, the first time a game's clock is advanced. */
public record GameStartedEvent() {
}
