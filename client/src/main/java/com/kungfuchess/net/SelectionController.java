package com.kungfuchess.net;

import java.util.HashSet;
import java.util.Set;

import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.input.BoardMapper;
import com.kungfuchess.model.Board;
import com.kungfuchess.model.Piece;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;
import com.kungfuchess.rules.PieceRules;

/**
 * The networked counterpart to {@code com.kungfuchess.input.Controller}: same two-click select/
 * deselect/reselect state machine, but resolved entirely locally against the most recently
 * received {@link GameSnapshot} -- no round trip needed just to highlight legal destinations --
 * and a completed selection sends a command over {@link ServerConnection} instead of calling an
 * engine directly. The server is still the sole authority: if it rejects the move, this class
 * doesn't find out and doesn't need to -- the next broadcast simply shows the true state, self-
 * correcting whatever this class optimistically assumed.
 *
 * <p>{@code myColor} (set once, after login) restricts what can even be *selected* -- only your
 * own pieces. This is a client-side nicety layered on top of the server's own authoritative
 * ownership check (see {@code GameMessageController}), not a replacement for it: without a color
 * (before login completes, or if both seats were already taken), nothing can ever be selected,
 * which is exactly the read-only behavior a spectator should get, for free.
 */
public final class SelectionController {

    private final MoveSender connection;
    private PieceColor myColor;
    private Position selectedCell;

    public SelectionController(MoveSender connection) {
        this.connection = connection;
    }

    public void setMyColor(PieceColor myColor) {
        this.myColor = myColor;
    }

    public void handleClick(int x, int y, GameSnapshot snapshot) {
        Position clicked = new Position(BoardMapper.toRow(y), BoardMapper.toCol(x));
        Board board = reconstructBoard(snapshot);

        if (!board.isInBounds(clicked)) {
            selectedCell = null;
            return;
        }

        if (selectedCell == null) {
            if (isOwnPieceAt(board, clicked)) {
                selectedCell = clicked;
            }
        } else if (clicked.equals(selectedCell)) {
            selectedCell = null;
        } else if (isOwnPieceAt(board, clicked)) {
            selectedCell = clicked;
        } else {
            connection.sendMove(selectedCell, clicked);
            selectedCell = null;
        }
    }

    public void handleJump(int x, int y) {
        connection.sendJump(new Position(BoardMapper.toRow(y), BoardMapper.toCol(x)));
    }

    public Position getSelectedPosition() {
        return selectedCell;
    }

    /** Mirrors {@code GameEngine.legalDestinationsFor}, computed locally against the latest snapshot. */
    public Set<Position> legalDestinations(GameSnapshot snapshot) {
        if (selectedCell == null) {
            return Set.of();
        }
        Board board = reconstructBoard(snapshot);
        Piece piece = board.getPiece(selectedCell);
        if (piece == null) {
            return Set.of();
        }

        Set<Position> destinations = new HashSet<>(PieceRules.getLegalDestinations(board, piece));
        destinations.removeIf(destination -> {
            Piece occupant = board.getPiece(destination);
            return occupant != null && occupant.getColor() == piece.getColor();
        });
        return destinations;
    }

    private boolean isOwnPieceAt(Board board, Position position) {
        Piece piece = board.getPiece(position);
        return piece != null && piece.getColor() == myColor;
    }

    /**
     * A lightweight {@link Board} rebuilt purely for {@link PieceRules} geometry -- state doesn't
     * matter here (IDLE is used uniformly), only kind/color/position.
     */
    private static Board reconstructBoard(GameSnapshot snapshot) {
        Board board = new Board(snapshot.boardRows(), snapshot.boardCols());
        for (PieceSnapshot pieceSnapshot : snapshot.pieces()) {
            board.addPiece(new Piece(
                    pieceSnapshot.color(), pieceSnapshot.kind(), pieceSnapshot.position(), PieceState.IDLE));
        }
        return board;
    }
}
