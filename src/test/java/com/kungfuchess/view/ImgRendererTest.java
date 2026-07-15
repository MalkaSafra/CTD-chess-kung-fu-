package com.kungfuchess.view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.kungfuchess.config.GameConfig;
import com.kungfuchess.engine.GameSnapshot;
import com.kungfuchess.engine.PieceSnapshot;
import com.kungfuchess.model.PieceColor;
import com.kungfuchess.model.PieceKind;
import com.kungfuchess.model.PieceState;
import com.kungfuchess.model.Position;

class ImgRendererTest {

    private final ImgRenderer renderer = new ImgRenderer("board.png", new PieceImageLoader(Path.of("pieces2")));

    @Test
    void renderedCanvasIncludesTheScoreBarsAboveAndBelowTheBoard() {
        GameSnapshot snapshot = new GameSnapshot(8, 8, List.of(), null, false, 0, 0, null, List.of(), Set.of());

        Img canvas = renderer.render(snapshot);

        assertEquals(800, canvas.get().getWidth());
        assertEquals(800 + 2 * GameConfig.SCORE_BAR_HEIGHT_PX, canvas.get().getHeight());
    }

    @Test
    void pieceInTheLastRowAndColumnStillFitsOnTheCanvas() {
        // The exact boundary case a naive off-by-one in cell math would blow up on.
        PieceSnapshot cornerPiece = new PieceSnapshot(
                PieceKind.KING, PieceColor.BLACK, PieceState.IDLE, 700, 700, 0);
        GameSnapshot snapshot = new GameSnapshot(8, 8, List.of(cornerPiece), null, false, 0, 0, null, List.of(), Set.of());

        assertDoesNotThrow(() -> renderer.render(snapshot));
    }

    @Test
    void selectedCellAndGameOverBannerDoNotThrow() {
        GameSnapshot selected = new GameSnapshot(8, 8, List.of(), new Position(3, 3), false, 0, 0, null, List.of(), Set.of());
        GameSnapshot over = new GameSnapshot(8, 8, List.of(), null, true, 9, 3, PieceColor.WHITE, List.of(), Set.of());

        assertDoesNotThrow(() -> renderer.render(selected));
        assertDoesNotThrow(() -> renderer.render(over));
    }
}
