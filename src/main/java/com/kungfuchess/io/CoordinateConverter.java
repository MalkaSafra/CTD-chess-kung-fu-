package com.kungfuchess.io;

import com.kungfuchess.config.GameConfig;

public final class CoordinateConverter {

    private CoordinateConverter() {
    }

    public static int toRow(int y) {
        return Math.floorDiv(y, GameConfig.CELL_SIZE_PX);
    }

    public static int toCol(int x) {
        return Math.floorDiv(x, GameConfig.CELL_SIZE_PX);
    }
}
