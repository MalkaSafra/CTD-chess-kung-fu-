package com.kungfuchess.server.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.kungfuchess.model.PieceColor;
import com.kungfuchess.server.game.GameRoom;
import com.kungfuchess.server.game.RoomRegistry;
import com.kungfuchess.server.game.RoomRole;
import com.kungfuchess.server.game.SessionAccountRegistry;

/**
 * Handles the Create/Join dialog's {@code /app/room/join}. There's only one action on the wire:
 * joining a room code that doesn't exist yet creates it (so the client's "Room" button covers both
 * Create and Join -- the player just types a code either way), and joining a code that already has
 * two seated players makes the joining session a read-only spectator instead of failing.
 */
@Controller
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    private final RoomRegistry roomRegistry;
    private final SessionAccountRegistry sessionAccountRegistry;

    public RoomController(RoomRegistry roomRegistry, SessionAccountRegistry sessionAccountRegistry) {
        this.roomRegistry = roomRegistry;
        this.sessionAccountRegistry = sessionAccountRegistry;
    }

    @MessageMapping("/room/join")
    @SendToUser("/queue/room")
    public RoomJoinResult handleJoin(RoomJoinCommand command, @Header("simpSessionId") String sessionId) {
        String username = sessionAccountRegistry.usernameOf(sessionId);
        if (username == null) {
            log.warn("Session {} requested to join a room before logging in", sessionId);
            return RoomJoinResult.failed("Not logged in.");
        }

        String roomCode = command.roomCode() == null ? "" : command.roomCode().trim();
        if (roomCode.isEmpty()) {
            return RoomJoinResult.failed("Room code must not be empty.");
        }

        RoomRole role = roomRegistry.joinOrCreate(roomCode, sessionId, username);
        GameRoom room = roomRegistry.findById(roomCode);
        log.info("Player '{}' joined room '{}' as {}", username, roomCode, role);

        return RoomJoinResult.joined(roomCode, role, room.usernameForColor(PieceColor.WHITE),
                room.usernameForColor(PieceColor.BLACK));
    }
}
