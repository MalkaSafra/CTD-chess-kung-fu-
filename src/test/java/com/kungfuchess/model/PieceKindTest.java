package com.kungfuchess.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PieceKindTest {

    @Test
    void materialValuesMatchStandardChessPointCounts() {
        assertEquals(1, PieceKind.PAWN.materialValue());
        assertEquals(3, PieceKind.KNIGHT.materialValue());
        assertEquals(3, PieceKind.BISHOP.materialValue());
        assertEquals(5, PieceKind.ROOK.materialValue());
        assertEquals(9, PieceKind.QUEEN.materialValue());
    }

    @Test
    void kingHasNoMaterialValue() {
        assertEquals(0, PieceKind.KING.materialValue());
    }
}
