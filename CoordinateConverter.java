public final class CoordinateConverter {

    public static final int CELL_SIZE_PX = 100;

    private CoordinateConverter() {
    }

    public static int toRow(int y) {
        return Math.floorDiv(y, CELL_SIZE_PX);
    }

    public static int toCol(int x) {
        return Math.floorDiv(x, CELL_SIZE_PX);
    }
}
