package com.kungfuchess.view;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Preloads every piece/state sprite set from a {@code pieces_classic}-shaped directory and hands back the
 * right frame for a given {@link PieceSnapshot}, timed by that state's real {@code config.json}
 * (frames_per_sec / is_loop) rather than one fixed guess for every piece and state.
 */
public final class PieceImageLoader {

    private static final String[] STATES = {"idle", "move", "jump", "short_rest", "long_rest"};

    private final Map<String, List<Img>> frames = new HashMap<>();
    private final Map<String, AnimationConfig> configs = new HashMap<>();

    public PieceImageLoader(Path piecesRoot) {
        for (PieceKind kind : PieceKind.values()) {
            for (PieceColor color : PieceColor.values()) {
                String code = folderCode(kind, color);
                for (String state : STATES) {
                    Path stateDir = piecesRoot.resolve(code).resolve("states").resolve(state);
                    String mapKey = key(code, state);
                    frames.put(mapKey, loadFrames(stateDir));
                    configs.put(mapKey, AnimationConfigLoader.load(stateDir.resolve("config.json")));
                }
            }
        }
    }

    public Img getFrame(PieceSnapshot piece) {
        String mapKey = key(folderCode(piece.kind(), piece.color()), stateFolder(piece.state()));

        List<Img> stateFrames = frames.get(mapKey);
        AnimationConfig config = configs.get(mapKey);

        return stateFrames.get(frameIndex(piece.stateElapsedMillis(), config, stateFrames.size()));
    }

    private int frameIndex(long elapsedMillis, AnimationConfig config, int frameCount) {
        long frameDurationMs = 1000L / config.getFramesPerSecond();
        int rawIndex = (int) (elapsedMillis / frameDurationMs);

        return config.isLoop() ? rawIndex % frameCount : Math.min(rawIndex, frameCount - 1);
    }

    private List<Img> loadFrames(Path stateDir) {
        List<Img> loaded = new ArrayList<>();
        Path sprites = stateDir.resolve("sprites");
        // Sprites (64x64) are smaller than a board cell (100x100); scale each frame up to fill
        // the cell at load time -- once, here -- rather than asking every drawOn() caller to know
        // the source sprite size and rescale it on every frame.
        Dimension cellSize = new Dimension(GameConfig.CELL_SIZE_PX, GameConfig.CELL_SIZE_PX);
        for (int i = 1; Files.exists(sprites.resolve(i + ".png")); i++) {
            loaded.add(new Img().read(sprites.resolve(i + ".png").toString(), cellSize, true, null));
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException("No sprite frames found under " + sprites);
        }
        return loaded;
    }

    private static String folderCode(PieceKind kind, PieceColor color) {
        // Asset folders are named Kind+Color (e.g. "PW" = pawn/white), the reverse of
        // BoardParser's Color+Kind board-token convention -- the two codes aren't interchangeable.
        return "" + kind.code() + (color == PieceColor.WHITE ? 'W' : 'B');
    }

    private static String stateFolder(PieceState state) {
        switch (state) {
            case IDLE:
                return "idle";
            case MOVING:
                return "move";
            case JUMPING:
                return "jump";
            case SHORT_REST:
                return "short_rest";
            case LONG_REST:
                return "long_rest";
            default:
                throw new IllegalArgumentException("No sprite folder for state: " + state);
        }
    }

    private static String key(String code, String state) {
        return code + "/" + state;
    }
}
