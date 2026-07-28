package com.kungfuchess.server.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.Position;
import com.kungfuchess.rules.MoveOutcome;
import com.kungfuchess.server.game.GameRoom;
import com.kungfuchess.server.game.RoomRegistry;

/**
 * The "thin gate" from the plan: no selection state, no per-session bookkeeping -- just
 * deserializes an already-resolved command and calls straight into whichever room's {@link
 * GameEngine} the command names. All the click-to-selection UX that {@code
 * com.kungfuchess.input.Controller} handles for the headless/local-GUI front doors is a
 * client-side concern here; by the time a command reaches this class, the client has already
 * decided exactly which move it wants, in which room.
 *
 * <p>Each room's {@code engine} is shared with {@link com.kungfuchess.server.game.GameClock}'s
 * scheduled tick thread, and {@code GameEngine}/{@code RealTimeArbiter} were never made
 * thread-safe -- they were built assuming single-threaded access, true on the local Swing client
 * (everything runs on the EDT) but not here. Every call into a room's engine is synchronized on
 * that same shared lock ({@code engine} itself) that {@code GameClock} also synchronizes on, so a
 * tick and a command for that room are never in the engine at the same time.
 *
 * <p>A rejection is reported back with {@code @SendToUser}, which routes to just the requesting
 * session even without real authentication (Stage 3) -- Spring falls back to the session id as
 * the "user" when there's no {@code Principal}. Returning {@code null} sends nothing, which is
 * why an accepted move/jump produces no reply at all.
 *
 * <p>Ownership is checked here, before the engine is ever touched: the room says which color (if
 * any) this session is allowed to move there, and a piece's own color has to match. An unknown
 * room, a spectator, or a session that never joined that room can't move anything; a seated
 * session still can't move the opponent's pieces. An empty source square is deliberately *not*
 * checked here -- that's left to fall through to the engine's own {@code EMPTY_SOURCE} rejection,
 * which is a more precise reason than this class could give.
 */
@Controller
public class GameMessageController {

    private static final Logger log = LoggerFactory.getLogger(GameMessageController.class);

    private final RoomRegistry roomRegistry;

    public GameMessageController(RoomRegistry roomRegistry) {
        this.roomRegistry = roomRegistry;
    }

    @MessageMapping("/move")
    @SendToUser("/queue/rejections")
    public RejectionNotice handleMove(MoveCommand command, @Header("simpSessionId") String sessionId) {
        GameRoom room = roomRegistry.findById(command.roomId());
        if (room == null) {
            return new RejectionNotice("UNKNOWN_ROOM");
        }

        Position source = new Position(command.fromRow(), command.fromCol());
        Position destination = new Position(command.toRow(), command.toCol());

        MoveOutcome outcome;
        GameEngine engine = room.engine();
        synchronized (engine) {
            RejectionNotice ownershipRejection = checkOwnership(room, sessionId, source);
            if (ownershipRejection != null) {
                return ownershipRejection;
            }
            outcome = engine.requestMove(source, destination);
        }
        return rejectionNoticeOrNull(command, outcome);
    }

    @MessageMapping("/jump")
    @SendToUser("/queue/rejections")
    public RejectionNotice handleJump(JumpCommand command, @Header("simpSessionId") String sessionId) {
        GameRoom room = roomRegistry.findById(command.roomId());
        if (room == null) {
            return new RejectionNotice("UNKNOWN_ROOM");
        }

        Position position = new Position(command.row(), command.col());

        MoveOutcome outcome;
        GameEngine engine = room.engine();
        synchronized (engine) {
            RejectionNotice ownershipRejection = checkOwnership(room, sessionId, position);
            if (ownershipRejection != null) {
                return ownershipRejection;
            }
            outcome = engine.requestJump(position);
        }
        return rejectionNoticeOrNull(command, outcome);
    }

    /** Must be called while holding the room's {@code engine} lock -- reads the live board. */
    private RejectionNotice checkOwnership(GameRoom room, String sessionId, Position source) {
        PieceColor playerColor = room.colorOf(sessionId);
        if (playerColor == null) {
            return new RejectionNotice("NOT_A_PLAYER");
        }

        Board board = room.engine().getBoard();
        Piece piece = board.getPiece(source);
        if (piece != null && piece.getColor() != playerColor) {
            return new RejectionNotice("NOT_YOUR_PIECE");
        }

        return null;
    }

    private RejectionNotice rejectionNoticeOrNull(Object command, MoveOutcome outcome) {
        if (outcome.isAccepted()) {
            return null;
        }
        log.debug("Rejected: {} ({})", command, outcome.getReason());
        return new RejectionNotice(outcome.getReason().name());
    }
}
