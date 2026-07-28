package com.kungfuchess.input;

/** Keywords of the "Commands:" text protocol that {@link CommandProcessor} parses. */
public final class CommandSyntax {

    private CommandSyntax() {
    }

    public static final String COMMAND_CLICK = "click";

    public static final String COMMAND_JUMP = "jump";

    public static final String COMMAND_WAIT = "wait";

    public static final String COMMAND_PRINT = "print";

    /** Currently the only supported argument to {@link #COMMAND_PRINT}. */
    public static final String PRINT_ARG_BOARD = "board";
}
