package com.kungfuchess.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PieceColorTest {

    @Test
    void oppositeOfWhiteIsBlackAndViceVersa() {
        assertEquals(PieceColor.BLACK, PieceColor.WHITE.opposite());
        assertEquals(PieceColor.WHITE, PieceColor.BLACK.opposite());
    }
}
