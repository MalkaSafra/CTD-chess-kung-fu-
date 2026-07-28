package com.kungfuchess.view;

/**
 * The parsed contents of one piece-animation-state's {@code config.json} (e.g.
 * {@code pieces_classic/PW/states/move/config.json}): how fast its sprite frames cycle, whether the
 * cycle loops, and which state it hands off to once it's done.
 */
public final class AnimationConfig {

    private final double speedMetersPerSecond;
    private final String nextStateWhenFinished;
    private final int framesPerSecond;
    private final boolean loop;

    public AnimationConfig(double speedMetersPerSecond, String nextStateWhenFinished,
                            int framesPerSecond, boolean loop) {
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.nextStateWhenFinished = nextStateWhenFinished;
        this.framesPerSecond = framesPerSecond;
        this.loop = loop;
    }

    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public String getNextStateWhenFinished() {
        return nextStateWhenFinished;
    }

    public int getFramesPerSecond() {
        return framesPerSecond;
    }

    public boolean isLoop() {
        return loop;
    }
}
