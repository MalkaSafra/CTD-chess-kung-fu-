package com.kungfuchess.engine;

/** Notified synchronously, in the same call, whenever a move or jump is recorded. */
@FunctionalInterface
public interface MoveListener {
    void onMoveRecorded(MoveRecord record);
}
