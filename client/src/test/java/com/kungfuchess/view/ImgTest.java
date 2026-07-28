package com.kungfuchess.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class ImgTest {

    @Test
    void drawRectDoesNotChangeImageDimensions() {
        Img board = new Img().read("board_classic.png");
        int width = board.get().getWidth();
        int height = board.get().getHeight();

        board.drawRect(10, 10, 50, 50, new Color(255, 215, 0, 180), 4);

        assertEquals(width, board.get().getWidth());
        assertEquals(height, board.get().getHeight());
    }

    @Test
    void fillRectDoesNotChangeImageDimensions() {
        Img board = new Img().read("board_classic.png");
        int width = board.get().getWidth();
        int height = board.get().getHeight();

        board.fillRect(0, 0, width, height, new Color(0, 0, 0, 150));

        assertEquals(width, board.get().getWidth());
        assertEquals(height, board.get().getHeight());
    }

    @Test
    void fillRectActuallyPaintsThePixelsItCovers() {
        Img board = new Img().read("board_classic.png");

        board.fillRect(0, 0, board.get().getWidth(), board.get().getHeight(), new Color(0, 0, 0, 255));

        assertEquals(0xFF000000, board.get().getRGB(5, 5), "fully opaque black should now cover every pixel");
        assertEquals(0xFF000000, board.get().getRGB(board.get().getWidth() - 1, board.get().getHeight() - 1));
    }

    @Test
    void scaledToStretchesToTheExactSizeWhenAspectIsNotKept() {
        Img square = Img.blank(100, 100, Color.BLACK);

        Img scaled = square.scaledTo(300, 150, false);

        assertEquals(300, scaled.get().getWidth());
        assertEquals(150, scaled.get().getHeight());
    }

    @Test
    void scaledToPreservesAspectRatioInsteadOfStretching() {
        Img square = Img.blank(100, 100, Color.BLACK);

        // Fitting a square into a 300x150 box while keeping aspect ratio should cap it at 150x150.
        Img scaled = square.scaledTo(300, 150, true);

        assertEquals(150, scaled.get().getWidth());
        assertEquals(150, scaled.get().getHeight());
    }

    @Test
    void copyProducesAnIndependentImageWithTheSameDimensions() {
        Img original = new Img().read("board_classic.png");

        Img copy = original.copy();

        assertNotSame(original.get(), copy.get());
        assertEquals(original.get().getWidth(), copy.get().getWidth());
        assertEquals(original.get().getHeight(), copy.get().getHeight());
    }
}
