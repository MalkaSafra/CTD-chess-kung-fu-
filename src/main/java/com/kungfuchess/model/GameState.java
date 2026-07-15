package com.kungfuchess.model;

public class GameState {

    private final Board board;
    private boolean gameOver;
    private PieceColor winner;

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

    /** The color whose king was captured, or {@code null} if the game isn't over. */
    public PieceColor getWinner() {
        return winner;
    }

    public void setWinner(PieceColor winner) {
        this.winner = winner;
    }
}
