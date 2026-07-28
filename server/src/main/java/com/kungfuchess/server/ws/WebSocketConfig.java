package com.kungfuchess.server.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket: clients connect at {@code /ws}, send commands to {@code /app/*} (routed
 * to a {@code @MessageMapping} handler), and subscribe to {@code /topic/*} to receive broadcasts
 * -- one {@code /topic/game/{roomId}} per {@link com.kungfuchess.server.game.GameRoom}, now that a
 * server can run several games at once (see {@link com.kungfuchess.server.game.RoomRegistry}).
 * The in-memory simple broker needs no config for these per-room sub-paths; any destination under
 * {@code /topic} just works. {@code /queue} is also enabled: it's what a
 * {@code @SendToUser} reply (see {@code GameMessageController}'s rejection notices) resolves to
 * once translated to a session-specific destination -- without it enabled here, the in-memory
 * broker doesn't recognize {@code /queue/**} at all and silently drops those messages, even
 * though the destination translation itself succeeds. No SockJS fallback: the real client is the
 * Java Swing app connecting with a native WebSocket, not a browser needing an XHR-polling
 * fallback for when a proxy blocks raw WebSocket.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }
}
