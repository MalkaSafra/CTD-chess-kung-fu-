package com.kungfuchess.app;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.input.Controller;
import com.kungfuchess.io.BoardParseException;
import com.kungfuchess.io.BoardParser;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.RuleEngine;
import com.kungfuchess.view.GameWindow;
import com.kungfuchess.view.ImgRenderer;
import com.kungfuchess.view.PieceImageLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;

/**
 * Composition root for the GUI entry point: only builds objects and wires their dependencies,
 * mirroring the existing text-mode {@link com.kungfuchess.Main} -- neither one contains any game
 * logic of its own.
 */
public final class GuiMain {

    private static final String STARTING_POSITION = """
            Board:
            bR bN bB bQ bK bB bN bR
            bP bP bP bP bP bP bP bP
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            wP wP wP wP wP wP wP wP
            wR wN wB wQ wK wB wN wR
            """;

    public static void main(String[] args) throws IOException {
        Board board = createStartingBoard();

        GameState gameState = new GameState(board);
        RuleEngine ruleEngine = new RuleEngine();
        RealTimeArbiter arbiter = new RealTimeArbiter();
        GameEngine engine = new GameEngine(gameState, ruleEngine, arbiter);
        Controller controller = new Controller(engine, System.out::println);

        PieceImageLoader imageLoader = new PieceImageLoader(Path.of("pieces_classic"));
        ImgRenderer renderer = new ImgRenderer("board_classic.png", imageLoader);
        GameWindow window = new GameWindow(engine, controller, renderer);

        window.start();
    }

    private static Board createStartingBoard() throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(STARTING_POSITION))) {
            return BoardParser.parse(reader).getBoard();
        } catch (BoardParseException e) {
            throw new IllegalStateException("Built-in starting position failed to parse: " + e.getCode(), e);
        }
    }
}
