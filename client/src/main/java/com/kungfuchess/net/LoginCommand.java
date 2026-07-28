package com.kungfuchess.net;

/** Mirrors {@code com.kungfuchess.server.ws.LoginCommand}. See {@link MoveCommand} for why. */
public record LoginCommand(String username, String password) {
}
