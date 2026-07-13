// Repository: https://github.com/MalkaSafra/CTD-chess-kung-fu-
package com.kungfuchess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.input.CommandProcessor;
import com.kungfuchess.input.Controller;
import com.kungfuchess.io.BoardParseException;
import com.kungfuchess.io.BoardParser;
import com.kungfuchess.io.ParsedBoard;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.GameState;
import com.kungfuchess.realtime.RealTimeArbiter;
import com.kungfuchess.rules.RuleEngine;

public final class Main {

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            ParsedBoard parsed = BoardParser.parse(reader);
            Board board = parsed.getBoard();

            GameState state = new GameState(board);
            RuleEngine ruleEngine = new RuleEngine();
            RealTimeArbiter arbiter = new RealTimeArbiter();
            GameEngine engine = new GameEngine(state, ruleEngine, arbiter);
            Controller controller = new Controller(engine, System.out::println);

            if (parsed.isCommandsSectionPresent()) {
                CommandProcessor commandProcessor = new CommandProcessor(controller);
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        commandProcessor.process(line);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        // A single malformed or invariant-violating command shouldn't take down
                        // the rest of an otherwise-valid command stream -- unlike a bad board
                        // fixture, there's no reason the remaining commands can't still run.
                        System.err.println(GameConfig.IO_ERROR_PREFIX + e.getMessage());
                    }
                }
            } else {
                controller.printBoard();
            }
        } catch (BoardParseException e) {
            System.out.println(GameConfig.ERROR_OUTPUT_PREFIX + e.getCode().name());
            System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println(GameConfig.IO_ERROR_PREFIX + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(GameConfig.IO_ERROR_PREFIX + e.getMessage());
            System.exit(1);
        }
    }
}
