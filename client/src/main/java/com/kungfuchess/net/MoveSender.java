package com.kungfuchess.net;

import com.kungfuchess.model.Position;

/** What {@link SelectionController} needs from {@link ServerConnection} -- small enough to fake in tests. */
public interface MoveSender {

    void sendMove(Position from, Position to);

    void sendJump(Position position);
}
