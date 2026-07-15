package com.kungfuchess.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AnimationConfigLoaderTest {

    @Test
    void loopingMoveAnimationIsParsed() {
        AnimationConfig config = AnimationConfigLoader.load(
                Path.of("pieces2", "PW", "states", "move", "config.json"));

        assertEquals(1.5, config.getSpeedMetersPerSecond());
        assertEquals("long_rest", config.getNextStateWhenFinished());
        assertEquals(12, config.getFramesPerSecond());
        assertTrue(config.isLoop());
    }

    @Test
    void nonLoopingJumpAnimationIsParsed() {
        AnimationConfig config = AnimationConfigLoader.load(
                Path.of("pieces2", "PW", "states", "jump", "config.json"));

        assertEquals(3.0, config.getSpeedMetersPerSecond());
        assertEquals("short_rest", config.getNextStateWhenFinished());
        assertEquals(8, config.getFramesPerSecond());
        assertFalse(config.isLoop());
    }

    @Test
    void missingFileFailsLoudlyInsteadOfReturningNull() {
        Path doesNotExist = Path.of("pieces2", "PW", "states", "idle", "missing.json");
        try {
            AnimationConfigLoader.load(doesNotExist);
            throw new AssertionError("expected failure for missing file");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
