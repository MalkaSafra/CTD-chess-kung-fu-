package com.kungfuchess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.Game;
import com.kungfuchess.io.BoardParseException;
import com.kungfuchess.io.BoardParser;
import com.kungfuchess.io.ParseErrorCode;
import com.kungfuchess.io.ParsedBoard;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;

/**
 * Covers the two seams that sit above individual piece rules: {@link Game}'s
 * real-time move/jump resolution (built on {@code MoveRequestQueue} and
 * {@code GameClock}, reached only through Game's public API), and
 * {@link BoardParser}'s text-fixture decoding. Both are exercised purely
 * through public entry points -- {@code Board}, {@code Game},
 * {@code BoardParser}, {@code GameConfig} -- with no reflection or
 * monkey-patching.
 */
class GameEngineTest {

    private static void click(Game game, int row, int col) {
        game.handleClick(col * GameConfig.CELL_SIZE_PX + 50, row * GameConfig.CELL_SIZE_PX + 50);
    }

    private static void jump(Game game, int row, int col) {
        game.handleJump(col * GameConfig.CELL_SIZE_PX + 50, row * GameConfig.CELL_SIZE_PX + 50);
    }

    @Nested
    class KungFuTimeMechanics {

        private Board board;
        private Game game;

        @BeforeEach
        void setUp() {
            board = new Board(3, 3);
            game = new Game(board, new PrintStream(new ByteArrayOutputStream()));
        }

        @Test
        void pieceDoesNotArriveUntilTheClockReachesCompletesAtMs() {
            board.setPiece(0, 0, Piece.fromToken("wR"));

            click(game, 0, 0);
            click(game, 0, 2); // 2-cell move onto an empty cell => 2000ms

            game.waitClock(1000);
            assertNotNull(board.getPiece(0, 0), "piece should still be at its source cell");
            assertNull(board.getPiece(0, 2), "piece should not have arrived yet");

            game.waitClock(1000);
            assertNull(board.getPiece(0, 0), "piece should have left its source cell");
            assertNotNull(board.getPiece(0, 2), "piece should have arrived at its destination");
        }

        @Test
        void airborneTrapReverseCapturesAnEnemyThatArrivesDuringTheJump() {
            board.setPiece(0, 0, Piece.fromToken("wR")); // defender
            board.setPiece(2, 0, Piece.fromToken("bR")); // attacker

            click(game, 2, 0);
            click(game, 0, 0); // attacker's capturing move: fixed 1000ms, completesAt = 1000

            game.waitClock(500);
            jump(game, 0, 0); // defender jumps mid-flight of the attack: completesAt = 500 + 1000 = 1500

            game.waitClock(500); // total elapsed 1000ms: the attacker's capture is now due

            Piece defender = board.getPiece(0, 0);
            assertNotNull(defender, "the airborne defender must still be on its cell");
            assertEquals("wR", defender.toString(), "the surviving piece must be the original defender");
            assertNull(board.getPiece(2, 0), "the arriving attacker must be destroyed entirely");

            game.waitClock(500); // let the jump itself land (total 1500ms)
            assertNotNull(board.getPiece(0, 0), "the jump landing must not disturb the surviving piece");
        }

        @Test
        void jumpingWithNoArrivalLandsBackWithNoEffect() {
            board.setPiece(0, 0, Piece.fromToken("wK"));

            jump(game, 0, 0);
            game.waitClock(1000);

            assertNotNull(board.getPiece(0, 0));
            assertEquals("wK", board.getPiece(0, 0).toString());
        }

        @Test
        void lateValidationCancelsAMoveOntoACellNowHeldByAFriendlyPiece() {
            board.setPiece(0, 0, Piece.fromToken("wR"));
            board.setPiece(1, 0, Piece.fromToken("wN"));

            click(game, 0, 0);
            click(game, 0, 2); // wR: (0,0)->(0,2), 2 cells onto an empty cell => 2000ms

            click(game, 1, 0);
            click(game, 0, 2); // wN: (1,0)->(0,2), valid L-shape, target still empty right now => 2000ms too

            // Both requests complete on the same tick; wR (queued first) lands, and wN's
            // late re-validation then finds its own target now friendly-occupied.
            game.waitClock(2000);

            assertEquals("wR", board.getPiece(0, 2).toString(), "the first-queued piece should have landed");
            assertNotNull(board.getPiece(1, 0), "the second piece's move must be cancelled");
            assertEquals("wN", board.getPiece(1, 0).toString(), "the cancelled piece must remain at its source");
        }

        @Test
        void capturingTheKingEndsTheGameAndFreezesAllFurtherMoves() {
            board.setPiece(0, 0, Piece.fromToken("wR"));
            board.setPiece(0, 2, Piece.fromToken("bK"));
            board.setPiece(1, 0, Piece.fromToken("wN"));

            click(game, 0, 0);
            click(game, 0, 2); // capture: fixed 1000ms

            game.waitClock(1000);
            assertTrue(board.isGameOver());
            assertEquals("wR", board.getPiece(0, 2).toString());

            click(game, 1, 0);
            click(game, 2, 2); // otherwise a legal knight move
            game.waitClock(2000);

            assertNotNull(board.getPiece(1, 0), "no move should be possible once the game is over");
            assertNull(board.getPiece(2, 2));
        }
    }

    @Nested
    class ParsingAndConfig {

        @Test
        void parsesAValidBoardCorrectly() throws IOException {
            String fixture = "Board:\nwK . . bK\n. . . .\nwR . . bR\nCommands:\nprint board\n";
            ParsedBoard parsed = BoardParser.parse(new BufferedReader(new StringReader(fixture)));

            Board board = parsed.getBoard();
            assertTrue(parsed.isCommandsSectionPresent());
            assertEquals(3, board.getRows());
            assertEquals(4, board.getCols());
            assertEquals("wK", board.getPiece(0, 0).toString());
            assertEquals("bK", board.getPiece(0, 3).toString());
            assertNull(board.getPiece(1, 0), "an empty-token cell must parse to null, not a placeholder piece");
            assertEquals("wR", board.getPiece(2, 0).toString());
            assertEquals("bR", board.getPiece(2, 3).toString());
        }

        @Test
        void bareBoardWithNoCommandsSectionIsReportedAsAbsent() throws IOException {
            String fixture = "Board:\nwK . . bK\n. . . .\nwR . . bR\n";
            ParsedBoard parsed = BoardParser.parse(new BufferedReader(new StringReader(fixture)));
            assertFalse(parsed.isCommandsSectionPresent());
        }

        @Test
        void unknownTokenThrowsWithTheCorrectErrorCode() {
            String fixture = "Board:\nwK xZ\n. .\nCommands:\n";
            BoardParseException exception = assertThrows(BoardParseException.class,
                    () -> BoardParser.parse(new BufferedReader(new StringReader(fixture))));
            assertEquals(ParseErrorCode.UNKNOWN_TOKEN, exception.getCode());
        }

        @Test
        void rowWidthMismatchThrowsWithTheCorrectErrorCode() {
            String fixture = "Board:\nwK . .\n. bK\nCommands:\n";
            BoardParseException exception = assertThrows(BoardParseException.class,
                    () -> BoardParser.parse(new BufferedReader(new StringReader(fixture))));
            assertEquals(ParseErrorCode.ROW_WIDTH_MISMATCH, exception.getCode());
        }

        @Test
        void emptyCellTokenFromConfigRoundTripsThroughParsingAndOutput() throws IOException {
            String fixture = "Board:\n" + GameConfig.EMPTY_CELL_TOKEN + "\nCommands:\n";
            ParsedBoard parsed = BoardParser.parse(new BufferedReader(new StringReader(fixture)));
            assertNull(parsed.getBoard().getPiece(0, 0));
            assertEquals(GameConfig.EMPTY_CELL_TOKEN, parsed.getBoard().toCanonicalString());
        }
    }
}
