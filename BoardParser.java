import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the "Board:" / "Commands:" text fixture from a {@link BufferedReader} and
 * produces a {@link ParsedBoard}.
 *
 * <p>This class owns the only knowledge of the input <em>syntax</em> in the whole
 * codebase; {@link Board} itself only knows about rows, columns, and {@link Piece}
 * placement, never about text formatting. A future {@code BinaryBoardParser} (or
 * any other format) could be introduced as a sibling class that decodes its own
 * input and calls the exact same {@code new Board(rows, cols)} /
 * {@code board.setPiece(...)} primitives used here. Nothing downstream --
 * {@link SelectionManager}, {@link MoveRequestQueue}, {@link CommandProcessor} --
 * would need to change, since none of them know or care how the {@link Board}
 * they were handed came to be populated.
 */
public final class BoardParser {

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
            if (trimmed.equalsIgnoreCase(GameConfig.BOARD_HEADER)) {
                continue;
            }
            if (trimmed.equalsIgnoreCase(GameConfig.COMMANDS_HEADER)) {
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
                if (!token.equals(GameConfig.EMPTY_CELL_TOKEN)) {
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
