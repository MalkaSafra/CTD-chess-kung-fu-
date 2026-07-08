package com.kungfuchess.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.Game;
import com.kungfuchess.io.BoardParseException;
import com.kungfuchess.io.BoardParser;
import com.kungfuchess.io.ParsedBoard;
import com.kungfuchess.model.Board;

public final class Main {

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            ParsedBoard parsed = BoardParser.parse(reader);
            Board board = parsed.getBoard();
            if (parsed.isCommandsSectionPresent()) {
                Game game = new Game(board, System.out);
                CommandProcessor processor = new CommandProcessor(game);
                String line;
                while ((line = reader.readLine()) != null) {
                    processor.process(line);
                }
            } else {
                System.out.println(board.toCanonicalString());
            }
        } catch (BoardParseException e) {
            System.out.println(GameConfig.ERROR_OUTPUT_PREFIX + e.getCode().name());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(GameConfig.IO_ERROR_PREFIX + e.getMessage());
            System.exit(1);
        }
    }
}
