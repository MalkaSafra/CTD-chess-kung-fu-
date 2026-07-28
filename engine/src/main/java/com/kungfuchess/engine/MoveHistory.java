package com.kungfuchess.engine;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Accumulates every accepted move/jump, in the order they were requested. */
final class MoveHistory {

    private final List<MoveRecord> records = new ArrayList<>();
    private final List<MoveListener> listeners = new ArrayList<>();

    void record(PieceColor color, PieceKind kind, Position source, Position destination, long timestampMs) {
        MoveRecord record = new MoveRecord(color, kind, source, destination, timestampMs);
        records.add(record);
        for (MoveListener listener : listeners) {
            listener.onMoveRecorded(record);
        }
    }

    List<MoveRecord> getAll() {
        return Collections.unmodifiableList(records);
    }

    void addListener(MoveListener listener) {
        listeners.add(listener);
    }
}
