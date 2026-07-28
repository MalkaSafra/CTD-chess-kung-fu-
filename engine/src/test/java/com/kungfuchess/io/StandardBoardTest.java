package com.kungfuchess.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;

class StandardBoardTest {

    @Test
    void producesAnEightByEightBoardWithBothBackRanksAndBothPawnRows() {
        Board board = StandardBoard.create();

        assertEquals(8, board.getRows());
        assertEquals(8, board.getCols());

        Piece whiteKing = board.getPiece(new Position(7, 4));
        assertEquals(PieceKind.KING, whiteKing.getKind());
        assertEquals(PieceColor.WHITE, whiteKing.getColor());

        Piece blackKing = board.getPiece(new Position(0, 4));
        assertEquals(PieceKind.KING, blackKing.getKind());
        assertEquals(PieceColor.BLACK, blackKing.getColor());

        for (int col = 0; col < 8; col++) {
            assertEquals(PieceKind.PAWN, board.getPiece(new Position(6, col)).getKind());
            assertEquals(PieceKind.PAWN, board.getPiece(new Position(1, col)).getKind());
        }
    }
}
