package com.kungfuchess.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

class CaptureLedgerTest {

    private Piece piece(PieceColor color, PieceKind kind) {
        return new Piece(color, kind, new Position(0, 0), PieceState.CAPTURED);
    }

    @Test
    void bothColorsStartAtZeroWithNoWinner() {
        CaptureLedger ledger = new CaptureLedger();

        assertEquals(0, ledger.getScore(PieceColor.WHITE));
        assertEquals(0, ledger.getScore(PieceColor.BLACK));
        assertNull(ledger.getWinner());
    }

    @Test
    void capturingAPieceCreditsTheOpposingColor() {
        CaptureLedger ledger = new CaptureLedger();

        ledger.recordCapture(piece(PieceColor.BLACK, PieceKind.QUEEN));

        assertEquals(9, ledger.getScore(PieceColor.WHITE));
        assertEquals(0, ledger.getScore(PieceColor.BLACK));
    }

    @Test
    void scoresAccumulateAcrossMultipleCaptures() {
        CaptureLedger ledger = new CaptureLedger();

        ledger.recordCapture(piece(PieceColor.BLACK, PieceKind.PAWN));
        ledger.recordCapture(piece(PieceColor.BLACK, PieceKind.ROOK));

        assertEquals(6, ledger.getScore(PieceColor.WHITE));
    }

    @Test
    void capturingAKingRecordsTheOpposingColorAsWinner() {
        CaptureLedger ledger = new CaptureLedger();

        ledger.recordCapture(piece(PieceColor.BLACK, PieceKind.KING));

        assertEquals(PieceColor.WHITE, ledger.getWinner());
    }

    @Test
    void capturingAKingDoesNotAwardMaterialPoints() {
        CaptureLedger ledger = new CaptureLedger();

        ledger.recordCapture(piece(PieceColor.BLACK, PieceKind.KING));

        assertEquals(0, ledger.getScore(PieceColor.WHITE));
    }
}
