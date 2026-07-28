package com.kungfuchess.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads one {@code config.json} into an {@link AnimationConfig}. The four fields it looks for are
 * matched by regex rather than parsed as general JSON: the file shape is fixed and owned by this
 * project's own asset pipeline, and drawing/asset code in this project may only depend on {@link Img}
 * -- pulling in a JSON library for four fixed fields isn't worth a new dependency.
 */
public final class AnimationConfigLoader {

    private static final Pattern SPEED = Pattern.compile("\"speed_m_per_sec\"\\s*:\\s*([0-9.]+)");
    private static final Pattern NEXT_STATE = Pattern.compile("\"next_state_when_finished\"\\s*:\\s*\"(\\w+)\"");
    private static final Pattern FRAMES_PER_SEC = Pattern.compile("\"frames_per_sec\"\\s*:\\s*([0-9]+)");
    private static final Pattern IS_LOOP = Pattern.compile("\"is_loop\"\\s*:\\s*(true|false)");

    private AnimationConfigLoader() {
    }

    public static AnimationConfig load(Path configJsonFile) {
        String json;
        try {
            json = Files.readString(configJsonFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read animation config: " + configJsonFile, e);
        }

        return new AnimationConfig(
                Double.parseDouble(match(SPEED, json, configJsonFile)),
                match(NEXT_STATE, json, configJsonFile),
                Integer.parseInt(match(FRAMES_PER_SEC, json, configJsonFile)),
                Boolean.parseBoolean(match(IS_LOOP, json, configJsonFile)));
    }

    private static String match(Pattern pattern, String json, Path source) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing " + pattern.pattern() + " in " + source);
        }
        return matcher.group(1);
    }
}
