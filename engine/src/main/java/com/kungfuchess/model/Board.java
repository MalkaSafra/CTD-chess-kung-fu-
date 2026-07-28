package com.kungfuchess.model;

import java.util.HashMap;
import java.util.Map;

public final class Board {

    private final int rows;
    private final int cols;
    private final Map<Position, Piece> occupancy = new HashMap<>();

    public Board(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        this.rows = rows;
        this.cols = cols;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean isInBounds(Position position) {
        return position.getRow() >= 0 && position.getRow() < rows
                && position.getCol() >= 0 && position.getCol() < cols;
    }

    public void addPiece(Piece piece) {
        Position position = piece.getPosition();
        if (!isInBounds(position)) {
            throw new IllegalArgumentException("Position out of bounds: " + position);
        }
        if (occupancy.containsKey(position)) {
            throw new IllegalStateException("Position already occupied: " + position);
        }
        occupancy.put(position, piece);
    }

    public Piece removePiece(Position position) {
        return occupancy.remove(position);
    }

    /** Relocates the piece at {@code from} to {@code to}, keeping the occupancy map and the
     * piece's own {@link Piece#getPosition()} in sync in one place. */
    public void movePiece(Position from, Position to) {
        Piece piece = removePiece(from);
        piece.setPosition(to);
        addPiece(piece);
    }

    public Piece getPiece(Position position) {
        return occupancy.get(position);
    }
}
