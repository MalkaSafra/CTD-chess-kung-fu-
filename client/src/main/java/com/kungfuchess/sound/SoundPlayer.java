package com.kungfuchess.sound;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads every {@link SoundEffect}'s clip once at construction (same eager-at-startup approach as
 * {@code PieceImageLoader}), then plays them back on demand. Pure {@code javax.sound.sampled} --
 * no new dependency -- which is why every asset under {@code sounds/} is a plain PCM {@code .wav}
 * rather than mp3/ogg (neither has built-in JDK decoding support).
 */
public final class SoundPlayer {

    private final Map<SoundEffect, Clip> clips = new EnumMap<>(SoundEffect.class);

    public SoundPlayer(Path soundsRoot) {
        for (SoundEffect effect : SoundEffect.values()) {
            clips.put(effect, loadClip(soundsRoot.resolve(effect.fileName())));
        }
    }

    /** Restarts {@code effect} from the beginning, even if it's still playing from a previous call. */
    public void play(SoundEffect effect) {
        Clip clip = clips.get(effect);
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    private static Clip loadClip(Path file) {
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(file.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new IllegalArgumentException("Cannot load sound: " + file, e);
        }
    }
}
