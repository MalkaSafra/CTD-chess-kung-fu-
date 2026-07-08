package com.kungfuchess.io;

import com.kungfuchess.model.Board;

public final class ParsedBoard {

    private final Board board;
    private final boolean commandsSectionPresent;

    public ParsedBoard(Board board, boolean commandsSectionPresent) {
        this.board = board;
        this.commandsSectionPresent = commandsSectionPresent;
    }

    public Board getBoard() {
        return board;
    }

    public boolean isCommandsSectionPresent() {
        return commandsSectionPresent;
    }
}
