package com.kungfuchess.model;

public class GameState {

    private final Board board;
    private boolean gameOver;

    public GameState(Board board) {
        this.board = board;
        this.gameOver = false;
    }

    public Board getBoard() {
        return board;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }
}
