package com.kungfuchess.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

class CombatResolverTest {

    private Board board;
    private CombatResolver combatResolver;

    @BeforeEach
    void setUp() {
        board = new Board(8, 8);
        combatResolver = new CombatResolver();
    }

    @Test
    void laterArrivalCapturesEarlierArrivalOnAnEmptySquare() {
        Position destination = new Position(4, 4);
        Piece earlyWhite = place(PieceColor.WHITE, PieceKind.ROOK, new Position(4, 0));
        Piece lateBlack = place(PieceColor.BLACK, PieceKind.ROOK, new Position(0, 4));

        CombatResolver.Arrival earlyArrival = new CombatResolver.Arrival(earlyWhite.getPosition(), earlyWhite, 500);
        CombatResolver.Arrival lateArrival = new CombatResolver.Arrival(lateBlack.getPosition(), lateBlack, 100);

        boolean kingCaptured = combatResolver.resolveContestedArrival(
                board, destination, Arrays.asList(earlyArrival, lateArrival));

        assertFalse(kingCaptured);
        assertEquals(lateBlack, board.getPiece(destination));
        assertEquals(PieceState.CAPTURED, earlyWhite.getState());
        assertEquals(PieceState.IDLE, lateBlack.getState());
        assertNull(board.getPiece(new Position(4, 0)));
    }

    @Test
    void tiedOvershootMutuallyDestroysBothArrivals() {
        Position destination = new Position(4, 4);
        Piece white = place(PieceColor.WHITE, PieceKind.ROOK, new Position(4, 0));
        Piece black = place(PieceColor.BLACK, PieceKind.ROOK, new Position(0, 4));

        CombatResolver.Arrival a = new CombatResolver.Arrival(white.getPosition(), white, 250);
        CombatResolver.Arrival b = new CombatResolver.Arrival(black.getPosition(), black, 250);

        combatResolver.resolveContestedArrival(board, destination, Arrays.asList(a, b));

        assertEquals(PieceState.CAPTURED, white.getState());
        assertEquals(PieceState.CAPTURED, black.getState());
        assertNull(board.getPiece(destination));
    }

    @Test
    void laterEatsEarlierPropagatesKingCapture() {
        Position destination = new Position(4, 4);
        Piece earlyKing = place(PieceColor.WHITE, PieceKind.KING, new Position(4, 0));
        Piece lateRook = place(PieceColor.BLACK, PieceKind.ROOK, new Position(0, 4));

        CombatResolver.Arrival earlyArrival = new CombatResolver.Arrival(earlyKing.getPosition(), earlyKing, 500);
        CombatResolver.Arrival lateArrival = new CombatResolver.Arrival(lateRook.getPosition(), lateRook, 100);

        boolean kingCaptured = combatResolver.resolveContestedArrival(
                board, destination, Arrays.asList(earlyArrival, lateArrival));

        assertTrue(kingCaptured);
    }

    @Test
    void friendlyDefenderAtDestinationStopsMoverOneSquareShort() {
        Piece mover = place(PieceColor.WHITE, PieceKind.ROOK, new Position(4, 0));
        Piece friendlyDefender = place(PieceColor.WHITE, PieceKind.PAWN, new Position(4, 3));

        boolean kingCaptured = combatResolver.resolveArrival(board, mover.getPosition(), new Position(4, 3), mover);

        assertFalse(kingCaptured);
        assertEquals(mover, board.getPiece(new Position(4, 2)), "mover should stop one square short of its ally");
        assertEquals(friendlyDefender, board.getPiece(new Position(4, 3)), "the blocking ally is untouched");
        assertNull(board.getPiece(new Position(4, 0)));
        assertEquals(PieceState.IDLE, mover.getState());
    }

    @Test
    void airborneEnemyDefenderCapturesArriver() {
        Piece jumper = place(PieceColor.BLACK, PieceKind.KING, new Position(4, 4));
        jumper.setState(PieceState.JUMPING);
        Piece attacker = place(PieceColor.WHITE, PieceKind.ROOK, new Position(4, 0));

        boolean kingCaptured = combatResolver.resolveArrival(board, attacker.getPosition(), new Position(4, 4), attacker);

        assertFalse(kingCaptured);
        assertEquals(PieceState.CAPTURED, attacker.getState());
        assertNull(board.getPiece(new Position(4, 0)));
        assertEquals(jumper, board.getPiece(new Position(4, 4)));
        assertEquals(PieceState.JUMPING, jumper.getState());
    }

    @Test
    void airborneFriendlyDefenderStopsArriverOneSquareShort() {
        Piece jumper = place(PieceColor.WHITE, PieceKind.KING, new Position(4, 4));
        jumper.setState(PieceState.JUMPING);
        Piece mover = place(PieceColor.WHITE, PieceKind.ROOK, new Position(4, 0));

        combatResolver.resolveArrival(board, mover.getPosition(), new Position(4, 4), mover);

        assertEquals(mover, board.getPiece(new Position(4, 3)));
        assertEquals(PieceState.IDLE, mover.getState());
        assertEquals(jumper, board.getPiece(new Position(4, 4)));
    }

    private Piece place(PieceColor color, PieceKind kind, Position position) {
        Piece piece = new Piece(color, kind, position, PieceState.IDLE);
        board.addPiece(piece);
        return piece;
    }
}
