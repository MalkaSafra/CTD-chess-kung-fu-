package com.kungfuchess.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SessionAccountRegistryTest {

    @Test
    void recordLoginThenLookUpReturnsTheUsername() {
        SessionAccountRegistry registry = new SessionAccountRegistry();

        registry.recordLogin("session-1", "alice");

        assertEquals("alice", registry.usernameOf("session-1"));
    }

    @Test
    void usernameOfReturnsNullForASessionThatNeverLoggedIn() {
        SessionAccountRegistry registry = new SessionAccountRegistry();

        assertNull(registry.usernameOf("never-logged-in"));
    }
}
