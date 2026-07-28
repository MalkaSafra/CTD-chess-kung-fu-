package com.kungfuchess.server.ws;

/** A first login for {@code username} auto-registers the account with this password; a repeat
 * login must match the stored (hashed) password. */
public record LoginCommand(String username, String password) {
}
