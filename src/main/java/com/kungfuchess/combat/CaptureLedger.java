package com.kungfuchess.combat;

import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks the running captured-material score for each color, and which color's king (if any) has
 * been captured -- the other color is the winner. Both facts are recorded from the same place:
 * the instant a piece is captured, since a captured king is just a special case of a capture.
 */
public final class CaptureLedger {

    private final Map<PieceColor, Integer> score = new EnumMap<>(PieceColor.class);
    private PieceColor winner;

    public CaptureLedger() {
        for (PieceColor color : PieceColor.values()) {
            score.put(color, 0);
        }
    }

    public void recordCapture(Piece capturedPiece) {
        PieceColor capturingColor = capturedPiece.getColor().opposite();
        score.merge(capturingColor, capturedPiece.getKind().materialValue(), Integer::sum);

        if (capturedPiece.getKind() == PieceKind.KING) {
            winner = capturingColor;
        }
    }

    public int getScore(PieceColor color) {
        return score.get(color);
    }

    public PieceColor getWinner() {
        return winner;
    }
}
