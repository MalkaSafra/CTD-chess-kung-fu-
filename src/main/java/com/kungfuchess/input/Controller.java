package com.kungfuchess.input;

import java.util.function.Consumer;

import com.kungfuchess.engine.GameEngine;
import com.kungfuchess.io.BoardPrinter;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.Position;

/**
 * Semantic input handler: knows what a click/jump/wait/print <em>means</em>
 * for the game (two-click select-then-move, pixel-to-cell mapping, clock
 * advancement, board rendering), and nothing about the text syntax used to
 * reach it. {@link CommandProcessor} owns that syntax and calls these
 * methods directly.
 */
public final class Controller {

    private final GameEngine engine;
    private final Consumer<String> output;
    private Position selectedCell = null;

    public Controller(GameEngine engine, Consumer<String> output) {
        this.engine = engine;
        this.output = output;
    }

    public void handleClick(int x, int y) {
        Position clickedPos = new Position(BoardMapper.toRow(y), BoardMapper.toCol(x));
        Board board = engine.getBoard();

        if (!board.isInBounds(clickedPos)) {
            selectedCell = null;
            return;
        }

        if (selectedCell == null) {
            selectedCell = clickedPos;
        } else if (clickedPos.equals(selectedCell)) {
            selectedCell = null;
        } else if (isFriendlyPieceAt(board, clickedPos)) {
            selectedCell = clickedPos;
        } else {
            engine.requestMove(selectedCell, clickedPos);
            selectedCell = null;
        }
    }

    private boolean isFriendlyPieceAt(Board board, Position position) {
        Piece selected = board.getPiece(selectedCell);
        Piece atPosition = board.getPiece(position);
        return selected != null && atPosition != null && atPosition.getColor() == selected.getColor();
    }

    public void handleJump(int x, int y) {
        Position position = new Position(BoardMapper.toRow(y), BoardMapper.toCol(x));
        engine.requestJump(position);
    }

    public void waitClock(long ms) {
        engine.waitClock(ms);
    }

    public Position getSelectedPosition() {
        return selectedCell;
    }

    public void printBoard() {
        output.accept(BoardPrinter.format(engine.getBoard()));
    }
}
