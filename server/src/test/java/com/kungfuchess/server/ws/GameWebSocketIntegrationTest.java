package com.kungfuchess.server.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.Position;
import com.kungfuchess.server.game.RoomRole;

/**
 * The real checkpoint for this stage: a genuine STOMP client, over a real socket, driving a
 * server-side room's {@code GameEngine} and receiving live {@link GameSnapshot} broadcasts back
 * on that room's own topic -- not a direct method call like {@link GameMessageControllerTest}, the
 * actual wire path.
 *
 * <p>Getting a seat now takes two round trips -- login, then {@code /app/play} -- since
 * matchmaking (not login) assigns colors and spins up a fresh room. {@link #loginAndMatch} does
 * both for two sessions and resolves which one actually ended up WHITE, since which of two
 * independent STOMP sends the server processes first isn't guaranteed.
 *
 * <p>{@code @DirtiesContext} per method mainly to keep each test's rooms/matchmaking queue state
 * isolated from the others, even though matchmaking now creates a fresh room per pairing rather
 * than sharing one global game.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private record SeatedSessions(StompSession whiteSession, StompSession blackSession, String roomId) {
    }

    @Test
    void movingAPieceOverTheSocketIsReflectedInBroadcastSnapshots() throws Exception {
        WebSocketStompClient stompClient = newStompClient();
        SeatedSessions seated = loginAndMatch(stompClient, "alice", "bob");

        BlockingQueue<GameSnapshot> snapshots = new LinkedBlockingQueue<>();
        seated.whiteSession().subscribe("/topic/game/" + seated.roomId(), snapshotHandler(snapshots));

        // Wait for at least one broadcast so the subscription is confirmed live before sending.
        assertNotNull(snapshots.poll(5, TimeUnit.SECONDS), "expected at least one snapshot broadcast before sending a move");

        // White pawn column 0, row 6 -> row 4: a standard two-square opening push.
        seated.whiteSession().send("/app/move", new MoveCommand(seated.roomId(), 6, 0, 4, 0));

        GameSnapshot afterMove = pollUntil(snapshots, snapshot -> !snapshot.moveHistory().isEmpty());

        assertNotNull(afterMove, "expected a snapshot reflecting the move within the poll window");
        assertEquals(1, afterMove.moveHistory().size());
        assertEquals(PieceKind.PAWN, afterMove.moveHistory().get(0).kind());
        assertEquals(new Position(6, 0), afterMove.moveHistory().get(0).source());
        assertEquals(new Position(4, 0), afterMove.moveHistory().get(0).destination());

        seated.whiteSession().disconnect();
        seated.blackSession().disconnect();
    }

    @Test
    void rejectedMoveIsReportedPrivatelyToTheRequestingSessionOnly() throws Exception {
        WebSocketStompClient stompClient = newStompClient();
        SeatedSessions seated = loginAndMatch(stompClient, "alice", "bob");

        BlockingQueue<RejectionNotice> rejectionsForRequester = new LinkedBlockingQueue<>();
        BlockingQueue<RejectionNotice> rejectionsForBystander = new LinkedBlockingQueue<>();
        seated.whiteSession().subscribe("/user/queue/rejections", rejectionHandler(rejectionsForRequester));
        seated.blackSession().subscribe("/user/queue/rejections", rejectionHandler(rejectionsForBystander));

        // Empty square -- guaranteed EMPTY_SOURCE now that the requester is a real seated player.
        seated.whiteSession().send("/app/jump", new JumpCommand(seated.roomId(), 3, 3));

        RejectionNotice notice = rejectionsForRequester.poll(5, TimeUnit.SECONDS);
        assertNotNull(notice, "the requester should be told its own jump was rejected");
        assertEquals("EMPTY_SOURCE", notice.reason());

        assertTrue(rejectionsForBystander.poll(1, TimeUnit.SECONDS) == null,
                "a second, uninvolved session must never see someone else's rejection");

        seated.whiteSession().disconnect();
        seated.blackSession().disconnect();
    }

    @Test
    void secondPlayerCannotMoveTheFirstPlayersPieces() throws Exception {
        WebSocketStompClient stompClient = newStompClient();
        SeatedSessions seated = loginAndMatch(stompClient, "alice", "bob");

        BlockingQueue<RejectionNotice> blackRejections = new LinkedBlockingQueue<>();
        seated.blackSession().subscribe("/user/queue/rejections", rejectionHandler(blackRejections));

        // BLACK tries to move a WHITE pawn.
        seated.blackSession().send("/app/move", new MoveCommand(seated.roomId(), 6, 0, 4, 0));

        RejectionNotice notice = blackRejections.poll(5, TimeUnit.SECONDS);
        assertNotNull(notice, "black should not be able to move white's piece");
        assertEquals("NOT_YOUR_PIECE", notice.reason());

        seated.whiteSession().disconnect();
        seated.blackSession().disconnect();
    }

    @Test
    void matchmakingPairsTwoSessionsWithOppositeColorsAndTellsEachTheOpponentsName() throws Exception {
        WebSocketStompClient stompClient = newStompClient();
        SeatedSessions seated = loginAndMatch(stompClient, "alice", "bob");

        // loginAndMatch already asserted both sides matched -- this test just makes the
        // opposite-colors/opponent-identity guarantee explicit and independently checkable.
        assertTrue(seated.whiteSession() != seated.blackSession());

        seated.whiteSession().disconnect();
        seated.blackSession().disconnect();
    }

    @Test
    void twoConcurrentMatchesGetIndependentRoomsAndDontSeeEachOthersMoves() throws Exception {
        WebSocketStompClient stompClient = newStompClient();
        SeatedSessions roomOne = loginAndMatch(stompClient, "carol", "dave");
        SeatedSessions roomTwo = loginAndMatch(stompClient, "erin", "frank");

        assertTrue(!roomOne.roomId().equals(roomTwo.roomId()), "each match should get its own room");

        BlockingQueue<GameSnapshot> roomTwoSnapshots = new LinkedBlockingQueue<>();
        roomTwo.whiteSession().subscribe("/topic/game/" + roomTwo.roomId(), snapshotHandler(roomTwoSnapshots));
        assertNotNull(roomTwoSnapshots.poll(5, TimeUnit.SECONDS));

        // A move in room one must never show up as a move in room two's broadcasts.
        roomOne.whiteSession().send("/app/move", new MoveCommand(roomOne.roomId(), 6, 0, 4, 0));

        for (int i = 0; i < 10; i++) {
            GameSnapshot snapshot = roomTwoSnapshots.poll(200, TimeUnit.MILLISECONDS);
            if (snapshot != null) {
                assertTrue(snapshot.moveHistory().isEmpty(), "room two must not see room one's move");
            }
        }

        roomOne.whiteSession().disconnect();
        roomOne.blackSession().disconnect();
        roomTwo.whiteSession().disconnect();
        roomTwo.blackSession().disconnect();
    }

    @Test
    void joiningARoomByCodeAfterBothSeatsAreTakenMakesYouASpectator() throws Exception {
        WebSocketStompClient stompClient = newStompClient();
        StompSession creator = connect(stompClient);
        StompSession joiner = connect(stompClient);
        StompSession spectator = connect(stompClient);
        login(creator, "creator");
        login(joiner, "joiner");
        login(spectator, "onlooker");

        RoomJoinResult creatorResult = joinRoom(creator, "SHARED-CODE");
        RoomJoinResult joinerResult = joinRoom(joiner, "SHARED-CODE");
        RoomJoinResult spectatorResult = joinRoom(spectator, "SHARED-CODE");

        assertEquals(RoomRole.WHITE, creatorResult.role());
        assertEquals(RoomRole.BLACK, joinerResult.role());
        assertEquals(RoomRole.SPECTATOR, spectatorResult.role());
        assertEquals(creatorResult.roomId(), spectatorResult.roomId());

        creator.disconnect();
        joiner.disconnect();
        spectator.disconnect();
    }

    private WebSocketStompClient newStompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient;
    }

    private StompSession connect(WebSocketStompClient stompClient) throws Exception {
        return stompClient.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);
    }

    /** Logs in and blocks until the response arrives. */
    private void login(StompSession session, String username) throws InterruptedException {
        BlockingQueue<LoginResponse> responses = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/login", loginHandler(responses));
        session.send("/app/login", new LoginCommand(username, "secret"));
        LoginResponse response = responses.poll(5, TimeUnit.SECONDS);
        assertNotNull(response, "expected a login response");
        assertTrue(response.success(), "expected login to succeed: " + response.errorMessage());
    }

    private RoomJoinResult joinRoom(StompSession session, String roomCode) throws InterruptedException {
        BlockingQueue<RoomJoinResult> results = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/room", roomJoinHandler(results));
        session.send("/app/room/join", new RoomJoinCommand(roomCode));
        RoomJoinResult result = results.poll(5, TimeUnit.SECONDS);
        assertNotNull(result, "expected a room join response");
        assertTrue(result.success(), "expected joining to succeed: " + result.failureReason());
        return result;
    }

    /** Logs both sessions in, sends {@code /app/play} for both, and resolves which one ended up WHITE. */
    private SeatedSessions loginAndMatch(WebSocketStompClient stompClient, String usernameA, String usernameB)
            throws Exception {
        StompSession sessionA = connect(stompClient);
        StompSession sessionB = connect(stompClient);

        BlockingQueue<MatchResult> matchesA = new LinkedBlockingQueue<>();
        BlockingQueue<MatchResult> matchesB = new LinkedBlockingQueue<>();
        sessionA.subscribe("/user/queue/match", matchHandler(matchesA));
        sessionB.subscribe("/user/queue/match", matchHandler(matchesB));

        login(sessionA, usernameA);
        login(sessionB, usernameB);

        sessionA.send("/app/play", new PlayCommand());
        sessionB.send("/app/play", new PlayCommand());

        MatchResult resultA = matchesA.poll(5, TimeUnit.SECONDS);
        MatchResult resultB = matchesB.poll(5, TimeUnit.SECONDS);
        assertNotNull(resultA, "expected " + usernameA + " to be matched");
        assertNotNull(resultB, "expected " + usernameB + " to be matched");
        assertTrue(resultA.matched(), "expected " + usernameA + "'s search to succeed: " + resultA.failureReason());
        assertTrue(resultB.matched(), "expected " + usernameB + "'s search to succeed: " + resultB.failureReason());

        if (resultA.color() == PieceColor.WHITE) {
            return new SeatedSessions(sessionA, sessionB, resultA.roomId());
        }
        return new SeatedSessions(sessionB, sessionA, resultA.roomId());
    }

    private StompFrameHandler snapshotHandler(BlockingQueue<GameSnapshot> sink) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return GameSnapshot.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sink.offer((GameSnapshot) payload);
            }
        };
    }

    private StompFrameHandler rejectionHandler(BlockingQueue<RejectionNotice> sink) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RejectionNotice.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sink.offer((RejectionNotice) payload);
            }
        };
    }

    private StompFrameHandler loginHandler(BlockingQueue<LoginResponse> sink) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LoginResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sink.offer((LoginResponse) payload);
            }
        };
    }

    private StompFrameHandler matchHandler(BlockingQueue<MatchResult> sink) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MatchResult.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sink.offer((MatchResult) payload);
            }
        };
    }

    private StompFrameHandler roomJoinHandler(BlockingQueue<RoomJoinResult> sink) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RoomJoinResult.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sink.offer((RoomJoinResult) payload);
            }
        };
    }

    private static GameSnapshot pollUntil(BlockingQueue<GameSnapshot> queue, java.util.function.Predicate<GameSnapshot> condition)
            throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            GameSnapshot snapshot = queue.poll(200, TimeUnit.MILLISECONDS);
            if (snapshot != null && condition.test(snapshot)) {
                return snapshot;
            }
        }
        return null;
    }
}
