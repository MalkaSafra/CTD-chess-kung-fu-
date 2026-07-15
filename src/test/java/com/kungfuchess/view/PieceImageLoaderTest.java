package com.kungfuchess.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;

class PieceImageLoaderTest {

    private final PieceImageLoader loader = new PieceImageLoader(Path.of("pieces2"));

    @Test
    void returnsAFrameForEveryKindColorAndState() {
        PieceState[] states = { PieceState.IDLE, PieceState.MOVING, PieceState.JUMPING,
                PieceState.SHORT_REST, PieceState.LONG_REST };

        for (PieceKind kind : PieceKind.values()) {
            for (PieceColor color : PieceColor.values()) {
                for (PieceState state : states) {
                    Img frame = loader.getFrame(new PieceSnapshot(kind, color, state, 0, 0, 0));
                    assertNotNull(frame.get());
                }
            }
        }
    }

    @Test
    void loopingMoveAnimationWrapsBackToEarlyFrames() {
        // white pawn "move": 12 fps -> ~83ms/frame, 5 frames; 500ms and 83ms both land on index 1.
        Img at500 = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.MOVING, 0, 0, 500));
        Img at83 = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.MOVING, 0, 0, 83));

        assertSame(at500.get(), at83.get(), "wrapping should land back on the same cached frame");
    }

    @Test
    void nonLoopingJumpAnimationHoldsOnLastFrame() {
        // white pawn "jump": 8 fps, not looping, 5 frames = 625ms total; both land clamped on index 4.
        Img beyondEnd = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.JUMPING, 0, 0, 700));
        Img atEnd = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.JUMPING, 0, 0, 625));

        assertSame(beyondEnd.get(), atEnd.get());
    }
}
