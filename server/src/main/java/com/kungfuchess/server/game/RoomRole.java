package com.kungfuchess.server.game;

import com.kungfuchess.model.PieceColor;

/** How a session is present in a {@link GameRoom}: seated as one color, or watching read-only. */
public enum RoomRole {
    WHITE, BLACK, SPECTATOR;

    static RoomRole forColor(PieceColor color) {
        return color == PieceColor.WHITE ? WHITE : BLACK;
    }
}
