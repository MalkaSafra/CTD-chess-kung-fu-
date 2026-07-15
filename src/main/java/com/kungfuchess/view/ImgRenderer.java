package com.kungfuchess.view;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.Position;

import java.awt.Color;
import java.awt.Dimension;

/**
 * Turns one {@link GameSnapshot} into a drawn {@link Img}: a score strip above and below the
 * board, board.png sized to the board's exact pixel dimensions, every piece's current sprite
 * frame, a highlight on the selected cell, and -- once the game ends -- a darkened board with a
 * centered game-over banner naming the winner. Every drawing call goes through {@link Img} --
 * nothing here touches {@code Graphics2D}/{@code ImageIO} directly.
 */
public final class ImgRenderer {

    private static final Color BAR_BACKGROUND = new Color(40, 40, 40);
    private static final Color BAR_TEXT_COLOR = Color.WHITE;
    private static final Color SELECTION_COLOR = new Color(255, 215, 0, 180);
    private static final Color LEGAL_DESTINATION_COLOR = new Color(50, 200, 50, 130);
    private static final Color DARKEN_OVERLAY = new Color(0, 0, 0, 180);
    private static final float SCORE_FONT_SIZE = 1.2f;
    private static final float TITLE_FONT_SIZE = 3.0f;
    private static final float WINNER_FONT_SIZE = 1.8f;

    private final String boardImagePath;
    private final PieceImageLoader imageLoader;

    public ImgRenderer(String boardImagePath, PieceImageLoader imageLoader) {
        this.boardImagePath = boardImagePath;
        this.imageLoader = imageLoader;
    }

    public Img render(GameSnapshot snapshot) {
        int boardWidth = snapshot.boardCols() * GameConfig.CELL_SIZE_PX;
        int boardHeight = snapshot.boardRows() * GameConfig.CELL_SIZE_PX;
        int totalHeight = boardHeight + 2 * GameConfig.SCORE_BAR_HEIGHT_PX;

        Img canvas = Img.blank(boardWidth, totalHeight, BAR_BACKGROUND);

        // board.png isn't exactly boardCols*boardRows*CELL_SIZE_PX in practice; stretching it to
        // that exact size at load time is simpler than teaching Img to draw its own checkerboard.
        Img board = new Img().read(boardImagePath, new Dimension(boardWidth, boardHeight), false, null);
        board.drawOn(canvas, 0, GameConfig.SCORE_BAR_HEIGHT_PX);

        drawLegalDestinations(canvas, snapshot);
        drawPieces(canvas, snapshot);
        drawSelectedCell(canvas, snapshot);
        drawScoreBars(canvas, snapshot);

        if (snapshot.gameOver()) {
            drawGameOver(canvas, snapshot, boardHeight);
        }

        return canvas;
    }

    private void drawPieces(Img canvas, GameSnapshot snapshot) {
        for (PieceSnapshot piece : snapshot.pieces()) {
            Img pieceImage = imageLoader.getFrame(piece);
            int x = (int) Math.round(piece.pixelX());
            int y = (int) Math.round(piece.pixelY()) + GameConfig.SCORE_BAR_HEIGHT_PX;
            pieceImage.drawOn(canvas, x, y);
        }
    }

    private void drawLegalDestinations(Img canvas, GameSnapshot snapshot) {
        for (Position destination : snapshot.legalDestinations()) {
            int x = destination.getCol() * GameConfig.CELL_SIZE_PX;
            int y = destination.getRow() * GameConfig.CELL_SIZE_PX + GameConfig.SCORE_BAR_HEIGHT_PX;
            canvas.fillRect(x, y, GameConfig.CELL_SIZE_PX, GameConfig.CELL_SIZE_PX, LEGAL_DESTINATION_COLOR);
        }
    }

    private void drawSelectedCell(Img canvas, GameSnapshot snapshot) {
        Position selected = snapshot.selectedPosition();
        if (selected == null) {
            return;
        }

        int x = selected.getCol() * GameConfig.CELL_SIZE_PX;
        int y = selected.getRow() * GameConfig.CELL_SIZE_PX + GameConfig.SCORE_BAR_HEIGHT_PX;
        canvas.drawRect(x, y, GameConfig.CELL_SIZE_PX, GameConfig.CELL_SIZE_PX, SELECTION_COLOR, 4);
    }

    private void drawScoreBars(Img canvas, GameSnapshot snapshot) {
        String blackText = "Black: " + snapshot.blackScore();
        String whiteText = "White: " + snapshot.whiteScore();

        drawCentered(canvas, blackText, SCORE_FONT_SIZE, GameConfig.SCORE_BAR_HEIGHT_PX - 12, BAR_TEXT_COLOR);
        drawCentered(canvas, whiteText, SCORE_FONT_SIZE, canvas.get().getHeight() - 12, BAR_TEXT_COLOR);
    }

    private void drawGameOver(Img canvas, GameSnapshot snapshot, int boardHeight) {
        canvas.fillRect(0, GameConfig.SCORE_BAR_HEIGHT_PX, canvas.get().getWidth(), boardHeight, DARKEN_OVERLAY);

        int titleY = GameConfig.SCORE_BAR_HEIGHT_PX + boardHeight / 2;
        drawCentered(canvas, "GAME OVER", TITLE_FONT_SIZE, titleY, Color.WHITE);

        if (snapshot.winner() != null) {
            drawCentered(canvas, winnerText(snapshot.winner()), WINNER_FONT_SIZE, titleY + 40, Color.WHITE);
        }
    }

    private void drawCentered(Img canvas, String text, float fontSize, int baselineY, Color color) {
        int x = (canvas.get().getWidth() - canvas.textWidth(text, fontSize)) / 2;
        canvas.putText(text, x, baselineY, fontSize, color, 1);
    }

    private String winnerText(PieceColor winner) {
        return (winner == PieceColor.WHITE ? "White" : "Black") + " wins";
    }
}
