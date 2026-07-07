import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BoardParser {

    private static final String HEADER = "Board:";

    private BoardParser() {
    }

    public static Board parse(BufferedReader reader) throws IOException {
        List<String> rowLines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!rowLines.isEmpty()) {
                    break;
                }
                continue;
            }
            if (trimmed.equalsIgnoreCase(HEADER)) {
                continue;
            }
            rowLines.add(trimmed);
        }

        if (rowLines.isEmpty()) {
            throw new IllegalArgumentException("No board rows found in input");
        }

        int cols = rowLines.get(0).split("\\s+").length;
        int rows = rowLines.size();
        Board board = new Board(rows, cols);

        for (int r = 0; r < rows; r++) {
            String[] tokens = rowLines.get(r).split("\\s+");
            if (tokens.length != cols) {
                throw new IllegalArgumentException(
                        "Row " + r + " has " + tokens.length + " columns, expected " + cols);
            }
            for (int c = 0; c < cols; c++) {
                String token = tokens[c];
                if (!token.equals(Board.EMPTY_TOKEN)) {
                    board.setPiece(r, c, Piece.fromToken(token));
                }
            }
        }

        return board;
    }
}
