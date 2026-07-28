package com.kungfuchess.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

class PieceImageLoaderTest {

    private final PieceImageLoader loader = new PieceImageLoader(Path.of("pieces_classic"));

    @Test
    void returnsAFrameForEveryKindColorAndState() {
        PieceState[] states = { PieceState.IDLE, PieceState.MOVING, PieceState.JUMPING,
                PieceState.SHORT_REST, PieceState.LONG_REST };

        for (PieceKind kind : PieceKind.values()) {
            for (PieceColor color : PieceColor.values()) {
                for (PieceState state : states) {
                    Img frame = loader.getFrame(new PieceSnapshot(kind, color, state, new Position(0, 0), 0, 0, 0));
                    assertNotNull(frame.get());
                }
            }
        }
    }

    @Test
    void loopingMoveAnimationWrapsBackToEarlyFrames() {
        // white pawn "move": 10 fps -> 100ms/frame, 5 frames (500ms full cycle); 600ms and 100ms both land on index 1.
        Img at600 = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.MOVING, new Position(0, 0), 0, 0, 600));
        Img at100 = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.MOVING, new Position(0, 0), 0, 0, 100));

        assertSame(at600.get(), at100.get(), "wrapping should land back on the same cached frame");
    }

    @Test
    void nonLoopingJumpAnimationHoldsOnLastFrame() {
        // white pawn "jump": 10 fps, not looping, 5 frames = 500ms total; both land clamped on index 4.
        Img beyondEnd = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.JUMPING, new Position(0, 0), 0, 0, 700));
        Img atEnd = loader.getFrame(new PieceSnapshot(PieceKind.PAWN, PieceColor.WHITE, PieceState.JUMPING, new Position(0, 0), 0, 0, 500));

        assertSame(beyondEnd.get(), atEnd.get());
    }
}
