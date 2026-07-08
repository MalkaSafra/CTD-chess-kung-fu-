package com.kungfuchess.engine;

public final class GameClock {

    private long elapsedMs = 0;

    public void advance(long ms) {
        elapsedMs += ms;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}
