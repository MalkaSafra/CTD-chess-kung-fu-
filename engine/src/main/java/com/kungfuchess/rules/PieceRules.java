package com.kungfuchess.rules;

import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.Position;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PieceRules {

    private static final int[][] ROOK_DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] BISHOP_DIRECTIONS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    private static final int[][] QUEEN_DIRECTIONS = concat(ROOK_DIRECTIONS, BISHOP_DIRECTIONS);
    private static final int[][] KNIGHT_OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] KING_OFFSETS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    private PieceRules() {
    }

    public static Set<Position> getLegalDestinations(Board board, Piece piece) {
        switch (piece.getKind()) {
            case ROOK:
                return scan(board, piece.getPosition(), ROOK_DIRECTIONS);

            case BISHOP:
                return scan(board, piece.getPosition(), BISHOP_DIRECTIONS);

            case QUEEN:
                return scan(board, piece.getPosition(), QUEEN_DIRECTIONS);

            case KNIGHT:
                return jumpOffsets(board, piece.getPosition(), KNIGHT_OFFSETS);

            case KING:
                return jumpOffsets(board, piece.getPosition(), KING_OFFSETS);

            case PAWN: {
                Set<Position> destinations = new HashSet<>();
                Position from = piece.getPosition();
                int direction = piece.getColor() == PieceColor.WHITE ? -1 : 1;

                Position forward = new Position(from.getRow() + direction, from.getCol());
                boolean forwardClear = board.isInBounds(forward) && board.getPiece(forward) == null;
                if (forwardClear) {
                    destinations.add(forward);
                }

                int startingRank = piece.getColor() == PieceColor.WHITE ? board.getRows() - 2 : 1;
                if (forwardClear && from.getRow() == startingRank) {
                    Position twoForward = new Position(from.getRow() + 2 * direction, from.getCol());
                    if (board.isInBounds(twoForward) && board.getPiece(twoForward) == null) {
                        destinations.add(twoForward);
                    }
                }

                for (int colOffset : new int[] {-1, 1}) {
                    Position diagonal = new Position(from.getRow() + direction, from.getCol() + colOffset);
                    if (board.isInBounds(diagonal) && board.getPiece(diagonal) != null) {
                        destinations.add(diagonal);
                    }
                }
                return destinations;
            }

            default:
                return new HashSet<>();
        }
    }

    /**
     * The straight-line cells strictly between {@code source} and {@code destination}, followed
     * by {@code destination} itself -- i.e. everything a sliding piece passes over to get there.
     * For a non-straight-line hop (a knight's L-shape) there is nothing in between, so the result
     * is just {@code destination}.
     */
    public static List<Position> pathBetween(Position source, Position destination) {
        List<Position> path = new ArrayList<>();
        if (source.equals(destination)) {
            return path;
        }

        int dRow = destination.getRow() - source.getRow();
        int dCol = destination.getCol() - source.getCol();
        boolean straightLine = dRow == 0 || dCol == 0 || Math.abs(dRow) == Math.abs(dCol);
        if (!straightLine) {
            path.add(destination);
            return path;
        }

        int rowStep = Integer.signum(dRow);
        int colStep = Integer.signum(dCol);
        Position cursor = new Position(source.getRow() + rowStep, source.getCol() + colStep);
        while (!cursor.equals(destination)) {
            path.add(cursor);
            cursor = new Position(cursor.getRow() + rowStep, cursor.getCol() + colStep);
        }
        path.add(destination);
        return path;
    }

    private static Set<Position> jumpOffsets(Board board, Position from, int[][] offsets) {
        Set<Position> destinations = new HashSet<>();
        for (int[] offset : offsets) {
            Position candidate = new Position(from.getRow() + offset[0], from.getCol() + offset[1]);
            if (board.isInBounds(candidate)) {
                destinations.add(candidate);
            }
        }
        return destinations;
    }

    private static int[][] concat(int[][] first, int[][] second) {
        int[][] combined = new int[first.length + second.length][];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    private static Set<Position> scan(Board board, Position from, int[][] directions) {
        Set<Position> destinations = new HashSet<>();
        for (int[] direction : directions) {
            int row = from.getRow() + direction[0];
            int col = from.getCol() + direction[1];
            Position candidate = new Position(row, col);
            while (board.isInBounds(candidate)) {
                destinations.add(candidate);
                if (board.getPiece(candidate) != null) {
                    break;
                }
                row += direction[0];
                col += direction[1];
                candidate = new Position(row, col);
            }
        }
        return destinations;
    }
}
