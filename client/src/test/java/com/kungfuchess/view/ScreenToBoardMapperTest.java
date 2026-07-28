package com.kungfuchess.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.kungfuchess.view.ScreenToBoardMapper.BoardPixel;
import com.kungfuchess.view.ScreenToBoardMapper.Frame;

class ScreenToBoardMapperTest {

    @Test
    void identityFrameSubtractsOnlyTheRenderMargins() {
        Frame frame = new Frame(800, 800, 800, 800, 0, 0);

        BoardPixel pixel = ScreenToBoardMapper.toBoardPixel(134, 180, frame);

        assertEquals(new BoardPixel(100, 100), pixel);
    }

    @Test
    void scaledDownDisplayIsProjectedBackToNativePixels() {
        // Native 800x800 shown at half size (400x400), no letterboxing.
        Frame frame = new Frame(800, 800, 400, 400, 0, 0);

        BoardPixel pixel = ScreenToBoardMapper.toBoardPixel(67, 90, frame);

        assertEquals(new BoardPixel(100, 100), pixel);
    }

    @Test
    void letterboxOffsetIsSubtractedBeforeScaling() {
        // Same half-size scaling as above, but the image is also letterboxed by (100, 50).
        Frame frame = new Frame(800, 800, 400, 400, 100, 50);

        BoardPixel pixel = ScreenToBoardMapper.toBoardPixel(167, 140, frame);

        assertEquals(new BoardPixel(100, 100), pixel);
    }

    @Test
    void clickLeftOfTheDisplayedImageReturnsNull() {
        Frame frame = new Frame(800, 800, 600, 800, 100, 0);

        assertNull(ScreenToBoardMapper.toBoardPixel(50, 400, frame));
    }

    @Test
    void clickBelowTheDisplayedImageReturnsNull() {
        Frame frame = new Frame(800, 800, 800, 600, 0, 100);

        assertNull(ScreenToBoardMapper.toBoardPixel(400, 750, frame));
    }

    @Test
    void clickPastTheRightOrBottomEdgeOfTheDisplayedImageReturnsNull() {
        Frame frame = new Frame(800, 800, 600, 800, 100, 0);

        assertNull(ScreenToBoardMapper.toBoardPixel(710, 400, frame));
    }
}
