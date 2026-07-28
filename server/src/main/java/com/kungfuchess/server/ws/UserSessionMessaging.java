package com.kungfuchess.server.ws;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Sends a message to one specific session's {@code /user/queue/*} destination from outside the
 * request/reply cycle a plain {@code @SendToUser} return value uses. {@code @SendToUser} replies
 * work with no {@code Principal} because Spring already knows the inbound message's session id;
 * a proactive push (e.g. matchmaking notifying a session that isn't the one whose message
 * triggered it) has no inbound message to inherit that from, so the session id has to be attached
 * explicitly via headers instead.
 */
final class UserSessionMessaging {

    private UserSessionMessaging() {
    }

    static void sendToSession(SimpMessagingTemplate messagingTemplate, String sessionId, String destination,
            Object payload) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload, headerAccessor.getMessageHeaders());
    }
}
