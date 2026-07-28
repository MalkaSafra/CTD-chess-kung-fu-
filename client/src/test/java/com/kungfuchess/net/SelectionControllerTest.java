package com.kungfuchess.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

class SelectionControllerTest {

    private record SentMove(Position from, Position to) {
    }

    private static final class RecordingSender implements MoveSender {
        final List<SentMove> movesSent = new ArrayList<>();
        final List<Position> jumpsSent = new ArrayList<>();

        @Override
        public void sendMove(Position from, Position to) {
            movesSent.add(new SentMove(from, to));
        }

        @Override
        public void sendJump(Position position) {
            jumpsSent.add(position);
        }
    }

    private static int pixelX(Position position) {
        return position.getCol() * GameConfig.CELL_SIZE_PX + 50;
    }

    private static int pixelY(Position position) {
        return position.getRow() * GameConfig.CELL_SIZE_PX + 50;
    }

    private static PieceSnapshot piece(PieceKind kind, PieceColor color, Position position) {
        return new PieceSnapshot(kind, color, PieceState.IDLE, position, 0, 0, 0);
    }

    private static GameSnapshot snapshotWith(PieceSnapshot... pieces) {
        return new GameSnapshot(8, 8, List.of(pieces), null, false, 0, 0, null, List.of(), Set.of(), List.of());
    }

    @Test
    void secondClickOnALegalDestinationSendsAMove() {
        Position rookAt = new Position(7, 0);
        GameSnapshot snapshot = snapshotWith(piece(PieceKind.ROOK, PieceColor.WHITE, rookAt));
        RecordingSender sender = new RecordingSender();
        SelectionController controller = new SelectionController(sender);
        controller.setMyColor(PieceColor.WHITE);

        controller.handleClick(pixelX(rookAt), pixelY(rookAt), snapshot);
        Position destination = new Position(7, 5);
        controller.handleClick(pixelX(destination), pixelY(destination), snapshot);

        assertEquals(List.of(new SentMove(rookAt, destination)), sender.movesSent);
        assertNull(controller.getSelectedPosition(), "selection clears once a move is sent, regardless of server outcome");
    }

    @Test
    void clickingTheSameCellTwiceDeselectsWithoutSendingAnything() {
        Position rookAt = new Position(7, 0);
        GameSnapshot snapshot = snapshotWith(piece(PieceKind.ROOK, PieceColor.WHITE, rookAt));
        RecordingSender sender = new RecordingSender();
        SelectionController controller = new SelectionController(sender);
        controller.setMyColor(PieceColor.WHITE);

        controller.handleClick(pixelX(rookAt), pixelY(rookAt), snapshot);
        controller.handleClick(pixelX(rookAt), pixelY(rookAt), snapshot);

        assertTrue(sender.movesSent.isEmpty());
        assertNull(controller.getSelectedPosition());
    }

    @Test
    void clickingAnotherOwnPieceReselectsInsteadOfMoving() {
        Position rookAt = new Position(7, 0);
        Position queenAt = new Position(7, 3);
        GameSnapshot snapshot = snapshotWith(
                piece(PieceKind.ROOK, PieceColor.WHITE, rookAt),
                piece(PieceKind.QUEEN, PieceColor.WHITE, queenAt));
        RecordingSender sender = new RecordingSender();
        SelectionController controller = new SelectionController(sender);
        controller.setMyColor(PieceColor.WHITE);

        controller.handleClick(pixelX(rookAt), pixelY(rookAt), snapshot);
        controller.handleClick(pixelX(queenAt), pixelY(queenAt), snapshot);

        assertTrue(sender.movesSent.isEmpty());
        assertEquals(queenAt, controller.getSelectedPosition());
    }

    @Test
    void jumpSendsTheClickedPositionDirectly() {
        RecordingSender sender = new RecordingSender();
        SelectionController controller = new SelectionController(sender);
        Position target = new Position(4, 4);

        controller.handleJump(pixelX(target), pixelY(target));

        assertEquals(List.of(target), sender.jumpsSent);
    }

    @Test
    void legalDestinationsAreComputedLocallyFromTheSnapshotWithNoNetworkCall() {
        Position rookAt = new Position(7, 0);
        GameSnapshot snapshot = snapshotWith(piece(PieceKind.ROOK, PieceColor.WHITE, rookAt));
        RecordingSender sender = new RecordingSender();
        SelectionController controller = new SelectionController(sender);
        controller.setMyColor(PieceColor.WHITE);

        controller.handleClick(pixelX(rookAt), pixelY(rookAt), snapshot);
        Set<Position> destinations = controller.legalDestinations(snapshot);

        assertTrue(destinations.contains(new Position(7, 7)), "rook should be able to slide along its rank");
        assertTrue(sender.movesSent.isEmpty() && sender.jumpsSent.isEmpty(), "computing highlights must not talk to the server");
    }

    @Test
    void cannotSelectTheOpponentsPiece() {
        Position blackRookAt = new Position(0, 0);
        GameSnapshot snapshot = snapshotWith(piece(PieceKind.ROOK, PieceColor.BLACK, blackRookAt));
        RecordingSender sender = new RecordingSender();
        SelectionController controller = new SelectionController(sender);
        controller.setMyColor(PieceColor.WHITE);

        controller.handleClick(pixelX(blackRookAt), pixelY(blackRookAt), snapshot);

        assertNull(controller.getSelectedPosition(), "clicking an opponent's piece must not select it");
    }

    @Test
    void withNoAssignedColorNothingCanEverBeSelected() {
        // Matches a spectator: login returned no color at all, e.g. because both seats were taken.
        Position rookAt = new Position(7, 0);
        GameSnapshot snapshot = snapshotWith(piece(PieceKind.ROOK, PieceColor.WHITE, rookAt));
        RecordingSender sender = new RecordingSender();
        SelectionController controller = new SelectionController(sender);
        // setMyColor deliberately never called.

        controller.handleClick(pixelX(rookAt), pixelY(rookAt), snapshot);

        assertNull(controller.getSelectedPosition());
    }
}
