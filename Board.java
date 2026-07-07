public final class Board {

    public static final String EMPTY_TOKEN = ".";

    private final int rows;
    private final int cols;
    private final Piece[][] grid;

    public Board(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.grid = new Piece[rows][cols];
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Piece getPiece(int row, int col) {
        return grid[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        grid[row][col] = piece;
    }

    public String toCanonicalString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c > 0) {
                    sb.append(' ');
                }
                Piece piece = grid[r][c];
                sb.append(piece == null ? EMPTY_TOKEN : piece.toString());
            }
            if (r < rows - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
