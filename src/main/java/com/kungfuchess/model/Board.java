package com.kungfuchess.model;

import com.kungfuchess.config.GameConfig;

public final class Board {

    private final int rows;
    private final int cols;
    private final Piece[][] grid;
    private boolean gameOver = false;

    public Board(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.grid = new Piece[rows][cols];
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Piece getPiece(int row, int col) {
        return grid[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        grid[row][col] = piece;
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        Piece moving = grid[fromRow][fromCol];
        removePiece(toRow, toCol);

        if (moving.getType() == PieceType.PAWN && toRow == promotionRow(moving.getColor())) {
            moving = new Queen(moving.getColor());
        }

        grid[toRow][toCol] = moving;
        grid[fromRow][fromCol] = null;
    }

    public void removePiece(int row, int col) {
        Piece captured = grid[row][col];
        if (captured != null && captured.getType() == PieceType.KING) {
            gameOver = true;
        }
        grid[row][col] = null;
    }

    private int promotionRow(Color color) {
        return color == Color.WHITE ? 0 : rows - 1;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isPathClear(int startRow, int startCol, int endRow, int endCol) {
        int rowStep = Integer.signum(endRow - startRow);
        int colStep = Integer.signum(endCol - startCol);
        int row = startRow + rowStep;
        int col = startCol + colStep;
        while (row != endRow || col != endCol) {
            if (grid[row][col] != null) {
                return false;
            }
            row += rowStep;
            col += colStep;
        }
        return true;
    }

    public String toCanonicalString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c > 0) {
                    sb.append(' ');
                }
                Piece piece = grid[r][c];
                sb.append(piece == null ? GameConfig.EMPTY_CELL_TOKEN : piece.toString());
            }
            if (r < rows - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
