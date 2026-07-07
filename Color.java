public enum Color {
    WHITE('w'),
    BLACK('b');

    private final char code;

    Color(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static Color fromCode(char code) {
        for (Color color : values()) {
            if (color.code == code) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color code: " + code);
    }
}
