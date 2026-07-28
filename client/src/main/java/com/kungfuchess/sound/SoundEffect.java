package com.kungfuchess.sound;

/** The game moments that have a distinct sound cue, and the asset file each one plays. */
public enum SoundEffect {
    MOVE("move.wav"),
    CAPTURE("capture.wav"),
    JUMP("jump.wav"),
    REJECTED("rejected.wav"),
    PROMOTION("promotion.wav"),
    GAME_START("game_start.wav"),
    GAME_END("game_end.wav");

    private final String fileName;

    SoundEffect(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
