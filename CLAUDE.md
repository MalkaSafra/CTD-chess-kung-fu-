# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"Kung Fu Chess": real-time chess where there are no turns — both players move simultaneously and
pieces need to physically travel across the board (with rest/cooldown after arriving) rather than
teleporting on a turn. The rules engine is otherwise standard chess movement geometry.

This is a Maven multi-module project with three modules under the root `pom.xml` (packaging `pom`,
Java 21):

- **`engine`** — headless, UI-agnostic game engine and rules. No Swing/Spring dependency (only
  `jackson-annotations`, for `Position`'s JSON deserialization). This is the single source of
  truth for game logic; both `client` and `server` depend on it and must never reimplement rules.
- **`client`** — Swing desktop GUI. Talks to `server` over STOMP/WebSocket; has no local
  `GameEngine` of its own in the networked path (`GuiMain`).
- **`server`** — Spring Boot app that owns every concurrently-running game (each an isolated
  `GameRoom`, its own `GameEngine`) and broadcasts each room's state over WebSocket.

There is no Maven wrapper (`mvnw`), and `mvn` is **not** on PATH in this environment — only
IntelliJ's bundled copy exists (e.g. under `JetBrains\IntelliJ IDEA <version>\plugins\maven\lib\
maven3\bin`), plus a populated `~/.m2` repo. Either invoke that binary directly, or build/run
through IntelliJ's Maven integration. Earlier project history used manual `javac` compilation, but
that no longer applies now that the Maven module structure is in place — this repo has no build
tool other than Maven.

## Common commands

Run from the repo root (module poms pin their `workingDirectory` back to the root for asset
loading, so `mvn` should generally be invoked from there, not from inside a module directory):

```
mvn test                              # build + run all tests, all modules
mvn -pl engine test                   # just the engine module's tests
mvn -pl client test
mvn -pl server test
mvn -Dtest=RuleEngineTest -pl engine test   # a single test class
mvn -pl server spring-boot:run        # start the server (must be running before the client connects)
mvn -pl client exec:java              # launch the Swing GUI client (prompts for username on stdin)
```

The engine also has a headless text-protocol entry point (`com.kungfuchess.Main`, in the `engine`
module) that reads a `Board:`/`Commands:` fixture from stdin — this is what the engine/rules test
fixtures and VPL-style grading harnesses exercise; see `ProtocolConfig`/`CommandSyntax` for the
text syntax.

Piece sprites/animation configs live in `pieces_classic/`, the board background in
`board_classic.png`, and sound effects in `sounds/` — all at the repo root, loaded via plain
relative filesystem paths (not classpath resources), which is why `workingDirectory` is pinned to
the repo root in the `engine` and `client` poms' surefire/exec config.

## Architecture

### Engine (`engine` module, package `com.kungfuchess`)

- `model` — plain data: `Board`, `Piece`, `PieceColor`, `PieceKind`, `PieceState`
  (`IDLE`/`MOVING`/`JUMPING`/`SHORT_REST`/`LONG_REST`/`CAPTURED`), `Position`, `GameState`.
- `rules` — `RuleEngine.validateMove` checks bounds/occupancy/geometry via `PieceRules` (per-kind
  legal-destination geometry) and returns a `MoveOutcome` (accepted, or rejected with a
  `MoveRejection` reason enum).
- `realtime` — `RealTimeArbiter` is where the "real-time" part actually lives: tracks in-flight
  `Motion`s (moves/jumps), advances them on each `advanceTime(ms, board)` tick, resolves contested
  arrivals (via `CombatResolver`) when two pieces land on the same square in the same tick, handles
  a mover getting blocked mid-path by a friendly piece that arrived first, and hands off to
  `RestTracker` once a piece lands. `MovementSpeeds` derives per-piece move/jump duration from the
  asset tree's `speed_m_per_sec` (falls back to fixed `GameConfig` constants if constructed with no
  args, e.g. for headless play with no asset tree on disk).
- `combat` — `CombatResolver` (capture/contested-arrival resolution), `RestTracker`
  (SHORT_REST/LONG_REST cooldown timing), `CaptureLedger` (per-color score + winner tracking).
- `engine` — `GameEngine` is the façade: `requestMove`/`requestJump` (validates via `RuleEngine`,
  then checks arbiter-level rejections like `PIECE_ALREADY_MOVING`/`PIECE_RESTING` before starting
  a `Motion`), `waitClock(ms)` (advances the arbiter and applies back-rank pawn promotion),
  `snapshot(selectedPosition)` (builds a read-only `GameSnapshot` for rendering — see below),
  `MoveHistory`/`MoveListener` for move-log tracking.
- `input` — `Controller`: the shared "what does a click/jump mean" state machine (two-click
  select-then-move, pixel↔cell mapping via `BoardMapper`), used directly by the headless
  `Main`/`CommandProcessor` text protocol. **Not** used by the networked client — see
  `client`'s own `SelectionController` below, which duplicates this state machine because it
  resolves selection locally against a `GameSnapshot` instead of calling a live `GameEngine`.
- `io` — the `Board:`/`Commands:` text fixture protocol (`BoardParser`, `BoardPrinter`,
  `ProtocolConfig`) used by the headless entry point and rules tests.
- `config` — `GameConfig`: cross-cutting gameplay-balance constants (durations, cell size in
  pixels). Layer-specific constants live in that layer's own config class instead
  (`view.RenderConfig`, `io.ProtocolConfig`, `input.CommandSyntax`) — don't add rendering or
  protocol constants here.

**Rendering never touches a live `Board`/`Piece` directly.** The engine exposes only a read-only
`GameSnapshot`/`PieceSnapshot` (records, built by the package-private `SnapshotFactory`) that
already contain resolved pixel positions, including mid-flight interpolation for a piece that's
mid-`Motion`. Always render from `engine.snapshot(...)`, never from `engine.getBoard()` directly.

### Client (`client` module, package `com.kungfuchess`)

- `net` — the networked counterpart to the engine's `input.Controller`: `ServerConnection` (STOMP
  client — connects, logs in, then subscribes to one room's own `/topic/game/{roomId}` once that
  room is known via `play()` (quick ELO±100 matchmaking, which spins up a fresh room server-side)
  or `joinRoom(code)` (the Create/Join dialog — a new code creates a room, an existing one joins
  it, and joining a room whose two seats are already taken makes you a `RoomRole.SPECTATOR`); also
  subscribes to `/user/queue/rejections`/`/user/queue/login`/`/user/queue/match`/`/user/queue/room`
  and sends `/app/move`/`/app/jump` tagged with whichever room was most recently entered) and
  `SelectionController` (same two-click select/deselect/reselect logic as `input.Controller`, but
  resolved locally against the latest `GameSnapshot` rather than a live engine — the server is the
  sole authority; a locally "accepted" selection just sends a command, and if the server rejects
  it, the next broadcast snapshot silently corrects the client's optimistic display). A spectator
  is simply a `SelectionController` with no color set — it was already read-only by construction,
  since nothing can be selected without a color to own it.
- `view` — `Img` (the **only** class allowed to touch AWT/Graphics2D/ImageIO — no other class may
  do raw image/pixel work), `AnimationConfigLoader` (reads each piece's `config.json` under
  `pieces_classic/<code>/states/<state>/`), `PieceImageLoader`, `ImgRenderer` (renders a
  `GameSnapshot`), `GameWindow` (Swing `JFrame`; the server owns the game clock, so this class just
  re-renders on each pushed snapshot rather than driving its own `Timer`), `LobbyDialog` (the
  post-login "Play" vs "Room" choice, and the room-code input prompt, as real Swing dialogs),
  `RenderConfig` (pixel/layout constants for this layer only).
- `sound` — `SoundPlayer`/`SoundEffect`, plays clips from `sounds/`.
- `app` — `GuiMain`: composition root only, no game logic. Prompts for a username/password on
  stdin (login is deliberately a console prompt, not a GUI screen), then wires `ServerConnection` →
  `SelectionController`/`GameWindow`. There is no local `GameEngine` in this path — the server
  is the sole authority.

### Server (`server` module, package `com.kungfuchess.server`)

Spring Boot app. A server can run any number of games at once — each is a `game.GameRoom`
(its own `GameEngine` + `GameEventPublisher` + two-seat/spectator bookkeeping), created and looked
up through `game.RoomRegistry`. There is no more one global game or one global 2-seat limit.

- `game.RoomRegistry` — owns every live `GameRoom`. Rooms come from two sources: `joinOrCreate(
  roomCode, sessionId, username)` (the Create/Join dialog — a code that doesn't exist yet is
  created on the spot, so "Create" and "Join" are the same wire call) and
  `createRoomForMatch(...)` (one fresh, isolated room per successful matchmaking pairing). Also
  tracks which room each session is currently in, for routing disconnects/reconnects/moves.
- `game.GameRoom` — one isolated game: its own `GameEngine`/`GameEventPublisher`, plus this room's
  own seat map (`join` fills WHITE then BLACK; a third distinct joiner becomes a read-only
  `RoomRole.SPECTATOR`) and spectator set. Replaces the old single-game `PlayerRegistry`'s seat half.
- `game.SessionAccountRegistry` — the identity half of what used to be `PlayerRegistry`: just
  session-to-username, genuinely global (a session logs in once, then may join any number of rooms).
- `game.GameClock` — `@Scheduled` (16ms tick) replacement for the GUI's Swing `Timer`: iterates
  every active room, advances that room's engine, and broadcasts its `GameSnapshot` to that room's
  own `/topic/game/{roomId}`.
- `game.DisconnectResignHandler` — the 20-second disconnect auto-resign, now keyed per session
  rather than assuming one game: on disconnect, looks up the session's room via `RoomRegistry`; a
  spectator is just dropped from that room's spectator set, a seated player gets the grace period
  before `GameEngine#resign` ends that room's game. Reconnecting in time (`tryReclaimSeat`) moves
  the seat within that same room.
- `ws.GameMessageController` — thin gate: deserializes an already-resolved move/jump command
  (tagged with a `roomId`) and calls straight into that room's `GameEngine`, after an ownership
  check via `GameRoom.colorOf` (a session can only move its own color's pieces in its own room). No
  selection/UX state here — that's entirely client-side.
- `ws.LoginController` — authenticates and records identity via `SessionAccountRegistry`; grants
  no seat by itself (that's matchmaking or `RoomController`'s job) except for a reconnect within
  `DisconnectResignHandler`'s grace period.
- `ws.MatchmakingController` — `/app/play`: strict ELO±100 pairing via `game.MatchmakingQueue` (no
  relaxation — a 60s-unmatched search is aborted by `ws.MatchmakingScheduler` and the client is
  told to retry manually), and on a match, calls `RoomRegistry.createRoomForMatch` so every pairing
  gets its own isolated room. No "game already in progress" gate anymore — any number of matches
  can run concurrently.
- `ws.RoomController` — `/app/room/join`: the Create/Join dialog's one wire call (see
  `RoomRegistry.joinOrCreate` above for the create-vs-join-vs-spectate rule).
- `ws.WebSocketConfig` — STOMP over WebSocket at `/ws`; `/app/*` routes to `@MessageMapping`
  handlers, `/topic/*` for per-room broadcasts, `/queue/*` for `@SendToUser` per-session replies
  (e.g. rejection notices). No SockJS fallback — the only real client is the Java Swing app.
- `bus` — a small pub/sub event bus (`GameEventBus`/`GameEventPublisher` +
  `GameStartedEvent`/`GameEndedEvent`/`MoveLoggedEvent`/`ScoreUpdatedEvent`) that each room's
  `GameEventPublisher` advances through, decoupling the tick from event fan-out.
  `GameEndedEvent` carries a `roomId` so a listener can resolve which room's players to credit now
  that several can be live at once.
- `account.Player`/`PlayerRepository` — JPA entity backed by SQLite (`sqlite-jdbc` +
  `hibernate-community-dialects`, since SQLite isn't a built-in Hibernate dialect). Passwords (if
  any) go through `spring-security-crypto` BCrypt only — this module deliberately does not pull in
  full Spring Security.
- `account.RatingService` — `@EventListener` on `GameEndedEvent`; standard ELO (K=32), resolving
  both players via `RoomRegistry.findById(event.roomId())` rather than a global registry.

**Threading**: `GameEngine`/`RealTimeArbiter` assume single-threaded access (true for the Swing
client's networked path, where nothing local touches the engine; true by construction for the
headless CLI). On the server, `GameClock`'s scheduled tick thread and each incoming WebSocket
command run on different thread pools, so every server-side call into a room's `engine` is
`synchronized` on that `engine` instance itself — preserve this locking discipline (one lock per
room, not a single global lock) when touching `GameClock` or any `@MessageMapping` handler; do not
add a new engine call path that skips the `synchronized (engine)` block.

## Working style for this project

Prefer small, incremental steps for any multi-part feature: implement one or two related classes,
compile, run the relevant module's tests (and the full suite as a regression check), then report
before continuing to the next step, rather than delivering a large multi-layer diff all at once.
