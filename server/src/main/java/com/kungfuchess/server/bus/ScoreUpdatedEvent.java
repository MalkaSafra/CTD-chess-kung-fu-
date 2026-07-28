package com.kungfuchess.server.bus;

import com.kungfuchess.model.PieceColor;

/** Published when a side's material score changes (i.e. that side captured a piece). */
public record ScoreUpdatedEvent(PieceColor color, int newScore) {
}
