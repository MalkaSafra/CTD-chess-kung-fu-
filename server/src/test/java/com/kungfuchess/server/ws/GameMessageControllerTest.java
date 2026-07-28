package com.kungfuchess.server.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.io.StandardBoard;
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
import com.kungfuchess.server.bus.GameEventBus;
import com.kungfuchess.server.bus.GameEventPublisher;
import com.kungfuchess.server.game.GameRoom;
import com.kungfuchess.server.game.RoomRegistry;

class GameMessageControllerTest {

    private static final String ROOM_ID = "room-1";
    private static final String WHITE_SESSION = "white-session";
    private static final String BLACK_SESSION = "black-session";

    private GameRoom newRoom(Board board) {
        GameEngine engine = new GameEngine(new GameState(board), new RuleEngine(), new RealTimeArbiter());
        GameEventPublisher publisher = new GameEventPublisher(engine, new GameEventBus(event -> { }), ROOM_ID);
        return new GameRoom(ROOM_ID, engine, publisher);
    }

    private GameRoom seatedRoom(Board board) {
        GameRoom room = newRoom(board);
        room.assignSeat(WHITE_SESSION, PieceColor.WHITE, "white-player");
        room.assignSeat(BLACK_SESSION, PieceColor.BLACK, "black-player");
        return room;
    }

    /** A registry that knows only about {@code room} -- these tests care about routing, not room lifecycle. */
    private RoomRegistry registryWith(GameRoom room) {
        RoomRegistry registry = mock(RoomRegistry.class);
        doReturn(room).when(registry).findById(ROOM_ID);
        return registry;
    }

    @Test
    void moveCommandTranslatesToAnEngineRequestMove() {
        GameRoom room = seatedRoom(StandardBoard.create());
        GameMessageController controller = new GameMessageController(registryWith(room));

        // White pawn column 0, row 6 -> row 4 (a standard two-square opening push).
        controller.handleMove(new MoveCommand(ROOM_ID, 6, 0, 4, 0), WHITE_SESSION);
        room.engine().waitClock(5000);

        assertEquals(PieceKind.PAWN, room.engine().getBoard().getPiece(new Position(4, 0)).getKind());
        assertNull(room.engine().getBoard().getPiece(new Position(6, 0)));
    }

    @Test
    void jumpCommandTranslatesToAnEngineRequestJump() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece(PieceColor.WHITE, PieceKind.ROOK, new Position(7, 0), PieceState.IDLE));
        GameRoom room = seatedRoom(board);
        GameMessageController controller = new GameMessageController(registryWith(room));

        controller.handleJump(new JumpCommand(ROOM_ID, 7, 0), WHITE_SESSION);

        assertEquals(PieceState.JUMPING, room.engine().getBoard().getPiece(new Position(7, 0)).getState());
    }

    @Test
    void acceptedMoveReturnsNoRejectionNotice() {
        GameRoom room = seatedRoom(StandardBoard.create());
        GameMessageController controller = new GameMessageController(registryWith(room));

        assertNull(controller.handleMove(new MoveCommand(ROOM_ID, 6, 0, 4, 0), WHITE_SESSION));
    }

    @Test
    void illegalMoveReturnsARejectionNoticeWithTheReason() {
        GameRoom room = seatedRoom(StandardBoard.create());
        GameMessageController controller = new GameMessageController(registryWith(room));

        // White pawn tries to move three squares diagonally to an empty square -- geometrically illegal.
        RejectionNotice notice = controller.handleMove(new MoveCommand(ROOM_ID, 6, 0, 5, 3), WHITE_SESSION);

        assertEquals(MoveRejection.ILLEGAL_PIECE_MOVE.name(), notice.reason());
    }

    @Test
    void jumpOnAnEmptySquareReturnsARejectionNotice() {
        GameRoom room = newRoom(new Board(8, 8));
        room.assignSeat(WHITE_SESSION, PieceColor.WHITE, "white-player");
        GameMessageController controller = new GameMessageController(registryWith(room));

        RejectionNotice notice = controller.handleJump(new JumpCommand(ROOM_ID, 3, 3), WHITE_SESSION);

        assertEquals(MoveRejection.EMPTY_SOURCE.name(), notice.reason());
    }

    @Test
    void sessionThatNeverLoggedInCannotMoveAnything() {
        GameRoom room = newRoom(StandardBoard.create());
        GameMessageController controller = new GameMessageController(registryWith(room));

        RejectionNotice notice = controller.handleMove(new MoveCommand(ROOM_ID, 6, 0, 4, 0), "stranger-session");

        assertEquals("NOT_A_PLAYER", notice.reason());
    }

    @Test
    void unknownRoomIsRejected() {
        RoomRegistry registry = mock(RoomRegistry.class);
        GameMessageController controller = new GameMessageController(registry);

        RejectionNotice notice = controller.handleMove(new MoveCommand("no-such-room", 6, 0, 4, 0), WHITE_SESSION);

        assertEquals("UNKNOWN_ROOM", notice.reason());
    }

    @Test
    void blackSessionCannotMoveAWhitePiece() {
        GameRoom room = seatedRoom(StandardBoard.create());
        GameMessageController controller = new GameMessageController(registryWith(room));

        RejectionNotice notice = controller.handleMove(new MoveCommand(ROOM_ID, 6, 0, 4, 0), BLACK_SESSION);

        assertEquals("NOT_YOUR_PIECE", notice.reason());
    }

    @Test
    void blackSessionCanMoveItsOwnPiece() {
        GameRoom room = seatedRoom(StandardBoard.create());
        GameMessageController controller = new GameMessageController(registryWith(room));

        // Black pawn column 0, row 1 -> row 3.
        RejectionNotice notice = controller.handleMove(new MoveCommand(ROOM_ID, 1, 0, 3, 0), BLACK_SESSION);

        assertNull(notice);
    }
}
