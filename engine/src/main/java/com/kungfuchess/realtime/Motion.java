package com.kungfuchess.realtime;

import com.kungfuchess.model.Piece;
import com.kungfuchess.model.Position;

public class Motion {

    private final Piece piece;
    private final Position source;
    private final Position destination;
    private final long totalDurationMs;
    private long elapsedMs;

    public Motion(Piece piece, Position source, Position destination, long totalDurationMs) {
        this.piece = piece;
        this.source = source;
        this.destination = destination;
        this.totalDurationMs = totalDurationMs;
        this.elapsedMs = 0;
    }

    public void addTime(long ms) {
        elapsedMs += ms;
    }

    public boolean isArrived() {
        return elapsedMs >= totalDurationMs;
    }

    public Piece getPiece() {
        return piece;
    }

    public Position getSource() {
        return source;
    }

    public Position getDestination() {
        return destination;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }
}
