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

    void record(PieceColor color, PieceKind kind, Position source, Position destination, long timestampMs) {
        records.add(new MoveRecord(color, kind, source, destination, timestampMs));
    }

    List<MoveRecord> getAll() {
        return Collections.unmodifiableList(records);
    }
}
