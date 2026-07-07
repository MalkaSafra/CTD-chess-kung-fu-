import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main {

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            Board board = BoardParser.parse(reader);
            System.out.println(board.toCanonicalString());
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
