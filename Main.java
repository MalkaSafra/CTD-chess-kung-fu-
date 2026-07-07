import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main {

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            Board board = BoardParser.parse(reader);
            System.out.println(board.toCanonicalString());
        } catch (BoardParseException e) {
            System.out.println("ERROR " + e.getCode().name());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
