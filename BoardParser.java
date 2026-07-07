import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BoardParser {

    private static final String BOARD_HEADER = "Board:";
    private static final String COMMANDS_HEADER = "Commands:";

    private BoardParser() {
    }

    public static ParsedBoard parse(BufferedReader reader) throws IOException {
        List<String> rowLines = new ArrayList<>();
        boolean commandsSectionPresent = false;
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!rowLines.isEmpty()) {
                    break;
                }
                continue;
            }
            if (trimmed.equalsIgnoreCase(BOARD_HEADER)) {
                continue;
            }
            if (trimmed.equalsIgnoreCase(COMMANDS_HEADER)) {
                commandsSectionPresent = true;
                break;
            }
            rowLines.add(trimmed);
        }

        if (rowLines.isEmpty()) {
            throw new BoardParseException(ParseErrorCode.EMPTY_BOARD, "No board rows found in input");
        }

        int cols = rowLines.get(0).split("\\s+").length;
        int rows = rowLines.size();
        Board board = new Board(rows, cols);

        for (int r = 0; r < rows; r++) {
            String[] tokens = rowLines.get(r).split("\\s+");
            if (tokens.length != cols) {
                throw new BoardParseException(ParseErrorCode.ROW_WIDTH_MISMATCH,
                        "Row " + r + " has " + tokens.length + " columns, expected " + cols);
            }
            for (int c = 0; c < cols; c++) {
                String token = tokens[c];
                if (!token.equals(Board.EMPTY_TOKEN)) {
                    board.setPiece(r, c, Piece.fromToken(token));
                }
            }
        }

        return new ParsedBoard(board, commandsSectionPresent);
    }
}

enum ParseErrorCode {
    UNKNOWN_TOKEN,
    ROW_WIDTH_MISMATCH,
    EMPTY_BOARD
}

final class BoardParseException extends RuntimeException {

    private final ParseErrorCode code;

    BoardParseException(ParseErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ParseErrorCode getCode() {
        return code;
    }
}

final class ParsedBoard {

    private final Board board;
    private final boolean commandsSectionPresent;

    ParsedBoard(Board board, boolean commandsSectionPresent) {
        this.board = board;
        this.commandsSectionPresent = commandsSectionPresent;
    }

    public Board getBoard() {
        return board;
    }

    public boolean isCommandsSectionPresent() {
        return commandsSectionPresent;
    }
}
