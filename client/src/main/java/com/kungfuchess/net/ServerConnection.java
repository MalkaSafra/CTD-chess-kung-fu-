package com.kungfuchess.net;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.model.Position;

/**
 * The client's half of the wire protocol {@code GameMessageController}/{@code GameClock}
 * implement server-side: connects once, then subscribes to one room's own {@code
 * /topic/game/{roomId}} once that room is known (see {@link #subscribeToRoom}) -- a server can run
 * several games at once now, so there's no single fixed topic to subscribe to at connect time
 * anymore. Sends resolved move/jump commands to {@code /app/move}/{@code /app/jump}, tagged with
 * whichever room {@link #play}/{@link #joinRoom} most recently entered. No selection logic here at
 * all -- see {@code SelectionController} for that; this class only knows how to talk to the server.
 */
public final class ServerConnection implements MoveSender {

    private final WebSocketStompClient stompClient;
    private StompSession session;
    private Consumer<GameSnapshot> onSnapshot;
    private volatile String currentRoomId;

    public ServerConnection() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    /** Blocks until connected (or throws) so callers don't have to handle a "not yet connected" state. */
    public void connect(String url, Consumer<GameSnapshot> onSnapshot, Consumer<RejectionNotice> onRejection) {
        this.onSnapshot = onSnapshot;
        try {
            session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() { })
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not connect to server at " + url, e);
        }

        // Private per-session queue -- see GameMessageController for why this can't ride a
        // room's shared broadcast.
        session.subscribe("/user/queue/rejections", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RejectionNotice.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                onRejection.accept((RejectionNotice) payload);
            }
        });
    }

    /**
     * Subscribes to one room's snapshot broadcasts and remembers it as the room {@link #sendMove}/
     * {@link #sendJump} target. Call once a room is known -- after {@link #play}, {@link
     * #joinRoom}, or a reclaimed seat on login.
     */
    public void subscribeToRoom(String roomId) {
        currentRoomId = roomId;
        session.subscribe("/topic/game/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return GameSnapshot.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                onSnapshot.accept((GameSnapshot) payload);
            }
        });
    }

    /**
     * Blocks for the server's response -- {@link LoginResponse#success()} is {@code false} on a
     * wrong password (retry with a new password is up to the caller). Must be called after
     * {@link #connect}. There is no seat/color here -- see {@link #play}/{@link #joinRoom} for
     * that. Blocking here (rather than a callback like move/jump results) is deliberate: the
     * caller needs the outcome before the game is meaningfully interactive anyway. Unsubscribes
     * once the response arrives so repeated calls (retrying a bad password) don't accumulate
     * subscriptions.
     */
    public LoginResponse login(String username, String password) {
        CompletableFuture<LoginResponse> future = new CompletableFuture<>();
        StompSession.Subscription subscription = session.subscribe("/user/queue/login", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LoginResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                future.complete((LoginResponse) payload);
            }
        });

        session.send("/app/login", new LoginCommand(username, password));

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Login failed for user " + username, e);
        } finally {
            subscription.unsubscribe();
        }
    }

    /**
     * Enters matchmaking and blocks until the server reports a result -- either a match (with a
     * color, a fresh room id, and the opponent's name/rating) or a failure ({@link
     * MatchResult#matched()} is {@code false}: the search timed out after 60s). The timeout here
     * is deliberately longer than the server's own 60s search window, so a real timeout response
     * has time to arrive rather than racing it.
     */
    public MatchResult play() {
        CompletableFuture<MatchResult> future = new CompletableFuture<>();
        StompSession.Subscription subscription = session.subscribe("/user/queue/match", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MatchResult.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                future.complete((MatchResult) payload);
            }
        });

        session.send("/app/play", new PlayCommand());

        try {
            return future.get(70, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Matchmaking request failed", e);
        } finally {
            subscription.unsubscribe();
        }
    }

    /**
     * Joins (creating first, if needed) the room with this code and blocks for the result --
     * {@link RoomJoinResult#role()} is WHITE/BLACK for the room's first two distinct joiners, or
     * SPECTATOR for anyone after that.
     */
    public RoomJoinResult joinRoom(String roomCode) {
        CompletableFuture<RoomJoinResult> future = new CompletableFuture<>();
        StompSession.Subscription subscription = session.subscribe("/user/queue/room", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RoomJoinResult.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                future.complete((RoomJoinResult) payload);
            }
        });

        session.send("/app/room/join", new RoomJoinCommand(roomCode));

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Joining room '" + roomCode + "' failed", e);
        } finally {
            subscription.unsubscribe();
        }
    }

    @Override
    public void sendMove(Position from, Position to) {
        session.send("/app/move", new MoveCommand(currentRoomId, from.getRow(), from.getCol(), to.getRow(), to.getCol()));
    }

    @Override
    public void sendJump(Position position) {
        session.send("/app/jump", new JumpCommand(currentRoomId, position.getRow(), position.getCol()));
    }
}
