package com.kungfuchess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;

class MoveHistoryTest {

    @Test
    void recordsAppendInOrder() {
        MoveHistory history = new MoveHistory();

        history.record(PieceColor.WHITE, PieceKind.PAWN, new Position(6, 4), new Position(4, 4), 0);
        history.record(PieceColor.BLACK, PieceKind.KNIGHT, new Position(0, 1), new Position(2, 2), 500);

        assertEquals(2, history.getAll().size());
        assertEquals(PieceColor.WHITE, history.getAll().get(0).color());
        assertEquals(PieceColor.BLACK, history.getAll().get(1).color());
        assertEquals(500, history.getAll().get(1).timestampMs());
    }

    @Test
    void jumpIsRecordedWithEqualSourceAndDestination() {
        MoveHistory history = new MoveHistory();

        history.record(PieceColor.WHITE, PieceKind.ROOK, new Position(3, 3), new Position(3, 3), 100);

        MoveRecord record = history.getAll().get(0);
        assertTrue(record.isJump());
    }

    @Test
    void plainMoveIsNotAJump() {
        MoveHistory history = new MoveHistory();

        history.record(PieceColor.WHITE, PieceKind.ROOK, new Position(3, 3), new Position(3, 6), 100);

        assertFalse(history.getAll().get(0).isJump());
    }
}
