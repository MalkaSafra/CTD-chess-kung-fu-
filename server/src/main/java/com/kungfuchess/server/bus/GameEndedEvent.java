package com.kungfuchess.server.bus;

import com.kungfuchess.model.PieceColor;

/** Published once, the tick a king is captured in {@code roomId}. {@code winner} captured it. */
public record GameEndedEvent(String roomId, PieceColor winner) {
}
