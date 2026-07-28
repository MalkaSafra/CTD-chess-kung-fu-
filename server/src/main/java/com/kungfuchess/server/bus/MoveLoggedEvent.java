package com.kungfuchess.server.bus;

import com.kungfuchess.engine.MoveRecord;

/** Published whenever the engine records a move or jump request. */
public record MoveLoggedEvent(MoveRecord record) {
}
