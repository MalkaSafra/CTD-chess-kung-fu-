package com.kungfuchess.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.MoveRejection;
import com.kungfuchess.rules.RuleEngine;

class ControllerTest {

    private static int pixelX(Position position) {
        return position.getCol() * GameConfig.CELL_SIZE_PX + 50;
    }

    private static int pixelY(Position position) {
        return position.getRow() * GameConfig.CELL_SIZE_PX + 50;
    }

    private Controller newController(Board board) {
        GameEngine engine = new GameEngine(new GameState(board), new RuleEngine(), new RealTimeArbiter());
        return new Controller(engine, message -> { });
    }

    private void place(Board board, PieceColor color, PieceKind kind, Position position) {
        board.addPiece(new Piece(color, kind, position, PieceState.IDLE));
    }

    @Test
    void geometricallyIllegalMoveNotifiesRejectionListenerWithTheReason() {
        Board board = new Board(8, 8);
        Position rookAt = new Position(7, 0);
        place(board, PieceColor.WHITE, PieceKind.ROOK, rookAt);
        Controller controller = newController(board);

        List<MoveRejection> rejections = new ArrayList<>();
        controller.addRejectionListener(rejections::add);

        controller.handleClick(pixelX(rookAt), pixelY(rookAt)); // select the rook
        Position offDiagonal = new Position(6, 1); // not a rook move
        controller.handleClick(pixelX(offDiagonal), pixelY(offDiagonal));

        assertEquals(List.of(MoveRejection.ILLEGAL_PIECE_MOVE), rejections);
    }

    @Test
    void acceptedMoveDoesNotNotifyRejectionListener() {
        Board board = new Board(8, 8);
        Position rookAt = new Position(7, 0);
        place(board, PieceColor.WHITE, PieceKind.ROOK, rookAt);
        Controller controller = newController(board);

        List<MoveRejection> rejections = new ArrayList<>();
        controller.addRejectionListener(rejections::add);

        controller.handleClick(pixelX(rookAt), pixelY(rookAt));
        Position destination = new Position(7, 5);
        controller.handleClick(pixelX(destination), pixelY(destination));

        assertTrue(rejections.isEmpty());
    }

    @Test
    void jumpOnAnEmptySquareNotifiesRejectionListener() {
        Board board = new Board(8, 8);
        Controller controller = newController(board);

        List<MoveRejection> rejections = new ArrayList<>();
        controller.addRejectionListener(rejections::add);

        Position empty = new Position(3, 3);
        controller.handleJump(pixelX(empty), pixelY(empty));

        assertEquals(List.of(MoveRejection.EMPTY_SOURCE), rejections);
    }
}
