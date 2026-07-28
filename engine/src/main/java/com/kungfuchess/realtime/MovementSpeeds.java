package com.kungfuchess.realtime;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Per-piece move/jump duration, derived from each piece's {@code speed_m_per_sec} in its "move"/
 * "jump" {@code config.json} (e.g. under {@code pieces_classic/}), converted to milliseconds using
 * an assumed {@link #CELL_SIZE_METERS} meters per board cell. That figure isn't arbitrary: the
 * asset set's move speed (1.5 m/s) reproduces {@link GameConfig#MOVE_DURATION_PER_CELL_MS} (1000ms)
 * exactly at 1.5 meters/cell, so today's move feel is unchanged -- only jump (previously a fixed
 * constant, never tuned to match) actually changes, from 1000ms to 500ms.
 *
 * <p>{@code engine} deliberately has no dependency on {@code client}/{@code view}, so this reads
 * the JSON itself (same technique as the view layer's own {@code AnimationConfigLoader}) rather
 * than reusing anything from there. The no-args constructor keeps every duration at the original
 * {@link GameConfig} fallback, so headless/text-protocol play -- which has no asset tree on disk --
 * is unaffected.
 */
public final class MovementSpeeds {

    private static final double CELL_SIZE_METERS = 1.5;

    private static final Pattern SPEED = Pattern.compile("\"speed_m_per_sec\"\\s*:\\s*([0-9.]+)");

    private final Map<String, Long> moveDurationMsPerCell = new HashMap<>();
    private final Map<String, Long> jumpDurationMs = new HashMap<>();

    /** Fixed {@link GameConfig} durations for every piece; no assets required. */
    public MovementSpeeds() {
    }

    /** Loads real per-piece speeds from {@code piecesRoot} (e.g. {@code Path.of("pieces_classic")}). */
    public MovementSpeeds(Path piecesRoot) {
        for (PieceKind kind : PieceKind.values()) {
            for (PieceColor color : PieceColor.values()) {
                String code = folderCode(kind, color);
                moveDurationMsPerCell.put(code, durationMs(piecesRoot, code, "move"));
                jumpDurationMs.put(code, durationMs(piecesRoot, code, "jump"));
            }
        }
    }

    public long moveDurationMsPerCell(PieceKind kind, PieceColor color) {
        return moveDurationMsPerCell.getOrDefault(folderCode(kind, color), GameConfig.MOVE_DURATION_PER_CELL_MS);
    }

    public long jumpDurationMs(PieceKind kind, PieceColor color) {
        return jumpDurationMs.getOrDefault(folderCode(kind, color), GameConfig.JUMP_DURATION_MS);
    }

    private static long durationMs(Path piecesRoot, String code, String state) {
        Path configFile = piecesRoot.resolve(code).resolve("states").resolve(state).resolve("config.json");
        String json;
        try {
            json = Files.readString(configFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read movement speed: " + configFile, e);
        }

        Matcher matcher = SPEED.matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No speed_m_per_sec found in " + configFile);
        }

        double speedMPerSec = Double.parseDouble(matcher.group(1));
        return Math.round(1000.0 * CELL_SIZE_METERS / speedMPerSec);
    }

    private static String folderCode(PieceKind kind, PieceColor color) {
        // Same Kind+Color convention as the view layer's PieceImageLoader (e.g. "PW" = pawn/white).
        return "" + kind.code() + (color == PieceColor.WHITE ? 'W' : 'B');
    }
}
