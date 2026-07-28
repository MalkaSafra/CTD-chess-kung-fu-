package com.kungfuchess.view;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.Position;

import java.awt.Color;
import java.awt.Dimension;

/**
 * Turns one {@link GameSnapshot} into a drawn {@link Img}: a black background, a score strip
 * above and below the board, a rank/file coordinate margin framing all four edges of the board,
 * board.png sized to the board's exact pixel dimensions, every piece's current sprite frame, a
 * highlight on the selected cell, and -- once the game ends -- a darkened board with a centered
 * game-over banner naming the winner. Every drawing call goes through {@link Img} -- nothing here
 * touches {@code Graphics2D}/{@code ImageIO} directly.
 */
public final class ImgRenderer {

    private static final Color CANVAS_BACKGROUND = Color.BLACK;
    private static final Color BAR_TEXT_COLOR = Color.WHITE;
    private static final Color COORDINATE_TEXT_COLOR = Color.WHITE;
    private static final Color SELECTION_COLOR = new Color(255, 215, 0, 180);
    private static final Color LEGAL_DESTINATION_COLOR = new Color(50, 200, 50, 130);
    private static final Color DARKEN_OVERLAY = new Color(0, 0, 0, 180);
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final float SCORE_FONT_SIZE = 1.8f;
    private static final float COORDINATE_FONT_SIZE = 1.3f;
    private static final float TITLE_FONT_SIZE = 3.0f;
    private static final float WINNER_FONT_SIZE = 1.8f;

    /**
     * Text is drawn at this many times its final size onto its own small patch, then shrunk back
     * down with {@link Img#scaledTo}'s bilinear interpolation before being composited onto the
     * canvas. An off-screen {@link java.awt.image.BufferedImage} only ever gets plain grayscale
     * antialiasing (no subpixel/LCD hinting the way an on-screen Swing component would) -- direct
     * small-size rendering looks visibly softer than the move list next to it as a result.
     * Supersampling like this, not a bigger point size, is what actually closes that gap.
     */
    private static final int TEXT_SUPERSAMPLE = 4;

    private final String boardImagePath;
    private final PieceImageLoader imageLoader;

    public ImgRenderer(String boardImagePath, PieceImageLoader imageLoader) {
        this.boardImagePath = boardImagePath;
        this.imageLoader = imageLoader;
    }

    public Img render(GameSnapshot snapshot) {
        int boardWidth = snapshot.boardCols() * GameConfig.CELL_SIZE_PX;
        int boardHeight = snapshot.boardRows() * GameConfig.CELL_SIZE_PX;
        int margin = RenderConfig.COORDINATE_MARGIN_PX;
        int canvasWidth = boardWidth + 2 * margin;
        int canvasHeight = boardHeight + 2 * RenderConfig.SCORE_BAR_HEIGHT_PX + 2 * margin;
        int boardOriginX = margin;
        int boardOriginY = RenderConfig.SCORE_BAR_HEIGHT_PX + margin;

        Img canvas = Img.blank(canvasWidth, canvasHeight, CANVAS_BACKGROUND);

        Img board = new Img().read(boardImagePath, new Dimension(boardWidth, boardHeight), false, null);
        board.drawOn(canvas, boardOriginX, boardOriginY);

        drawCoordinates(canvas, snapshot, boardOriginX, boardOriginY, boardWidth, boardHeight);
        drawLegalDestinations(canvas, snapshot, boardOriginX, boardOriginY);
        drawPieces(canvas, snapshot, boardOriginX, boardOriginY);
        drawSelectedCell(canvas, snapshot, boardOriginX, boardOriginY);
        drawScoreBars(canvas, snapshot);

        if (snapshot.gameOver()) {
            drawGameOver(canvas, boardOriginX, boardOriginY, boardWidth, boardHeight, snapshot.winner());
        }

        return canvas;
    }

    private void drawPieces(Img canvas, GameSnapshot snapshot, int boardOriginX, int boardOriginY) {
        for (PieceSnapshot piece : snapshot.pieces()) {
            Img pieceImage = imageLoader.getFrame(piece);
            int x = (int) Math.round(piece.pixelX()) + boardOriginX;
            int y = (int) Math.round(piece.pixelY()) + boardOriginY;
            pieceImage.drawOn(canvas, x, y);
        }
    }

    private void drawLegalDestinations(Img canvas, GameSnapshot snapshot, int boardOriginX, int boardOriginY) {
        for (Position destination : snapshot.legalDestinations()) {
            int x = destination.getCol() * GameConfig.CELL_SIZE_PX + boardOriginX;
            int y = destination.getRow() * GameConfig.CELL_SIZE_PX + boardOriginY;
            canvas.fillRect(x, y, GameConfig.CELL_SIZE_PX, GameConfig.CELL_SIZE_PX, LEGAL_DESTINATION_COLOR);
        }
    }

    private void drawSelectedCell(Img canvas, GameSnapshot snapshot, int boardOriginX, int boardOriginY) {
        Position selected = snapshot.selectedPosition();
        if (selected == null) {
            return;
        }

        int x = selected.getCol() * GameConfig.CELL_SIZE_PX + boardOriginX;
        int y = selected.getRow() * GameConfig.CELL_SIZE_PX + boardOriginY;
        canvas.drawRect(x, y, GameConfig.CELL_SIZE_PX, GameConfig.CELL_SIZE_PX, SELECTION_COLOR, 4);
    }

    /**
     * Files (a, b, c, ...) centered above and below the board, ranks (numbered top-to-bottom the
     * same way {@code GameWindow}'s move list already does: {@code boardRows - row}) centered to
     * its left and right -- framing all four edges, matching the reference layout the user supplied.
     */
    private void drawCoordinates(Img canvas, GameSnapshot snapshot, int boardOriginX, int boardOriginY,
                                  int boardWidth, int boardHeight) {
        int margin = RenderConfig.COORDINATE_MARGIN_PX;
        int topStripCenterY = boardOriginY - margin / 2;
        int bottomStripCenterY = boardOriginY + boardHeight + margin / 2;

        for (int col = 0; col < snapshot.boardCols(); col++) {
            String file = String.valueOf((char) ('a' + col));
            int cellCenterX = boardOriginX + col * GameConfig.CELL_SIZE_PX + GameConfig.CELL_SIZE_PX / 2;
            drawCenteredAt(canvas, file, COORDINATE_FONT_SIZE, cellCenterX, topStripCenterY, COORDINATE_TEXT_COLOR);
            drawCenteredAt(canvas, file, COORDINATE_FONT_SIZE, cellCenterX, bottomStripCenterY, COORDINATE_TEXT_COLOR);
        }

        int leftStripCenterX = boardOriginX - margin / 2;
        int rightStripCenterX = boardOriginX + boardWidth + margin / 2;

        for (int row = 0; row < snapshot.boardRows(); row++) {
            String rank = String.valueOf(snapshot.boardRows() - row);
            int cellCenterY = boardOriginY + row * GameConfig.CELL_SIZE_PX + GameConfig.CELL_SIZE_PX / 2;
            drawCenteredAt(canvas, rank, COORDINATE_FONT_SIZE, leftStripCenterX, cellCenterY, COORDINATE_TEXT_COLOR);
            drawCenteredAt(canvas, rank, COORDINATE_FONT_SIZE, rightStripCenterX, cellCenterY, COORDINATE_TEXT_COLOR);
        }
    }

    private void drawScoreBars(Img canvas, GameSnapshot snapshot) {
        String blackText = "Black: " + snapshot.blackScore();
        String whiteText = "White: " + snapshot.whiteScore();

        drawCentered(canvas, blackText, SCORE_FONT_SIZE, RenderConfig.SCORE_BAR_HEIGHT_PX - 14, BAR_TEXT_COLOR);
        drawCentered(canvas, whiteText, SCORE_FONT_SIZE, canvas.get().getHeight() - 14, BAR_TEXT_COLOR);
    }

    private void drawGameOver(Img canvas, int boardOriginX, int boardOriginY, int boardWidth, int boardHeight,
                               PieceColor winner) {
        canvas.fillRect(boardOriginX, boardOriginY, boardWidth, boardHeight, DARKEN_OVERLAY);

        int titleY = boardOriginY + boardHeight / 2;
        drawCentered(canvas, "GAME OVER", TITLE_FONT_SIZE, titleY, Color.WHITE);

        if (winner != null) {
            drawCentered(canvas, winnerText(winner), WINNER_FONT_SIZE, titleY + 40, Color.WHITE);
        }
    }

    private void drawCentered(Img canvas, String text, float fontSize, int baselineY, Color color) {
        int x = (canvas.get().getWidth() - canvas.textWidth(text, fontSize)) / 2;
        drawTextCrisp(canvas, text, fontSize, x, baselineY, color);
    }

    /** Like {@link #drawCentered}, but centered on an arbitrary point rather than the full canvas width. */
    private void drawCenteredAt(Img canvas, String text, float fontSize, int centerX, int centerY, Color color) {
        int x = centerX - canvas.textWidth(text, fontSize) / 2;
        int y = centerY + 5; // small fudge: text's y is a baseline, not a vertical center
        drawTextCrisp(canvas, text, fontSize, x, y, color);
    }

    /**
     * Draws {@code text} onto its own transparent, {@link #TEXT_SUPERSAMPLE}x-oversized patch,
     * shrinks that patch back down, then composites it onto {@code canvas} at ({@code x},
     * {@code baselineY}) -- see {@link #TEXT_SUPERSAMPLE} for why this beats drawing directly.
     */
    private void drawTextCrisp(Img canvas, String text, float fontSize, int x, int baselineY, Color color) {
        int ss = TEXT_SUPERSAMPLE;
        float ssFontSize = fontSize * ss;
        int padding = ss * 4; // room inside the patch so antialiasing fringe never touches its edge

        int patchWidth = canvas.textWidth(text, ssFontSize) + 2 * padding;
        int patchHeight = Math.round(ssFontSize * 12 * 1.6f);
        int baselineInPatch = Math.round(ssFontSize * 12 * 1.1f);

        Img patch = Img.blank(patchWidth, patchHeight, TRANSPARENT);
        patch.putText(text, padding, baselineInPatch, ssFontSize, color, 1);

        Img shrunk = patch.scaledTo(Math.max(1, patchWidth / ss), Math.max(1, patchHeight / ss), false);

        int drawX = x - padding / ss;
        int drawY = baselineY - Math.round(baselineInPatch / (float) ss);
        shrunk.drawOn(canvas, drawX, drawY);
    }

    private String winnerText(PieceColor winner) {
        return (winner == PieceColor.WHITE ? "White" : "Black") + " wins";
    }
}
