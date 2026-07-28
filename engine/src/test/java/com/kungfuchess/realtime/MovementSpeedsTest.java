package com.kungfuchess.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;

class MovementSpeedsTest {

    @Test
    void noArgsConstructorFallsBackToGameConfigDefaults() {
        MovementSpeeds speeds = new MovementSpeeds();

        assertEquals(GameConfig.MOVE_DURATION_PER_CELL_MS, speeds.moveDurationMsPerCell(PieceKind.QUEEN, PieceColor.BLACK));
        assertEquals(GameConfig.JUMP_DURATION_MS, speeds.jumpDurationMs(PieceKind.QUEEN, PieceColor.BLACK));
    }

    @Test
    void loadsRealSpeedFromThePiecesClassicAssetTree() {
        // move: 1.5 m/s at the calibrated 1.5 meters/cell reproduces today's 1000ms/cell exactly.
        // jump: 3.0 m/s at 1.5 meters/cell is 500ms, half of the old fixed 1000ms constant.
        MovementSpeeds speeds = new MovementSpeeds(Path.of("pieces_classic"));

        assertEquals(1000L, speeds.moveDurationMsPerCell(PieceKind.PAWN, PieceColor.WHITE));
        assertEquals(500L, speeds.jumpDurationMs(PieceKind.PAWN, PieceColor.WHITE));

        assertEquals(1000L, speeds.moveDurationMsPerCell(PieceKind.KNIGHT, PieceColor.BLACK));
        assertEquals(500L, speeds.jumpDurationMs(PieceKind.KNIGHT, PieceColor.BLACK));
    }
}
