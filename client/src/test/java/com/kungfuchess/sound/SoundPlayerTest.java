package com.kungfuchess.sound;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SoundPlayerTest {

    @Test
    void loadsAClipForEverySoundEffect() {
        assertDoesNotThrow(() -> new SoundPlayer(Path.of("sounds")));
    }

    @Test
    void playingEveryEffectTwiceInARowDoesNotThrow() {
        // Twice, to exercise SoundPlayer.play() restarting a clip that's still playing/just played.
        SoundPlayer player = new SoundPlayer(Path.of("sounds"));

        for (SoundEffect effect : SoundEffect.values()) {
            assertDoesNotThrow(() -> player.play(effect));
            assertDoesNotThrow(() -> player.play(effect));
        }
    }
}
