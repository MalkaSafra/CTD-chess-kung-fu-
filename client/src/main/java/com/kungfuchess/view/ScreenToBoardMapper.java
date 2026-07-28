package com.kungfuchess.view;

/**
 * Pure pixel math translating a mouse-event coordinate on {@link GameWindow}'s displayed image
 * (which may be scaled and letterboxed to whatever size the user has resized the window to) back
 * into the renderer's native, board-local pixel space -- the same space
 * {@link com.kungfuchess.input.BoardMapper} expects.
 *
 * <p>Kept separate from {@code BoardMapper} itself: this half is specific to how {@link
 * GameWindow} scales/frames {@link ImgRenderer}'s output on screen; {@code BoardMapper}'s half
 * (board-local pixel -> row/col) is shared with the text-mode protocol, which has no screen to
 * scale at all.
 */
public final class ScreenToBoardMapper {

    private ScreenToBoardMapper() {
    }

    /**
     * Everything needed to translate a click on the currently-displayed image back to the
     * renderer's native pixel space: the native (unscaled) render size, the size it was actually
     * scaled to for display, and the letterbox offset within the label.
     */
    public record Frame(int nativeWidth, int nativeHeight, int displayedWidth, int displayedHeight,
                         int letterboxOffsetX, int letterboxOffsetY) {
    }

    /** A pixel coordinate in the renderer's native, board-local space -- board.png's own
     * coordinate space, before {@code BoardMapper} converts it to a row/col. */
    public record BoardPixel(int x, int y) {
    }

    /**
     * Returns the board-local pixel the click landed on, or {@code null} if the click landed in
     * the letterbox margin rather than on the rendered image at all.
     */
    public static BoardPixel toBoardPixel(int eventX, int eventY, Frame frame) {
        int localX = eventX - frame.letterboxOffsetX();
        int localY = eventY - frame.letterboxOffsetY();
        if (localX < 0 || localY < 0 || localX >= frame.displayedWidth() || localY >= frame.displayedHeight()) {
            return null;
        }

        int nativeX = (int) (localX * (frame.nativeWidth() / (double) frame.displayedWidth()));
        int nativeY = (int) (localY * (frame.nativeHeight() / (double) frame.displayedHeight()));

        // The board is inset from the canvas origin by the left coordinate margin and by the top
        // score bar plus the top coordinate margin -- clicks must be shifted back by both before
        // they reach BoardMapper, or every click would land on the wrong cell (or, for a click
        // actually inside one of those margins, on a cell that doesn't exist -- BoardMapper's
        // floorDiv still produces a row/col, but Controller's isInBounds check rejects it, same as
        // any other out-of-bounds click already does).
        int boardX = nativeX - RenderConfig.COORDINATE_MARGIN_PX;
        int boardY = nativeY - RenderConfig.SCORE_BAR_HEIGHT_PX - RenderConfig.COORDINATE_MARGIN_PX;

        return new BoardPixel(boardX, boardY);
    }
}
