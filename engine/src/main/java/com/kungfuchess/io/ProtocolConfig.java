package com.kungfuchess.io;

/** Syntax constants for the "Board:" text fixture format and its VPL-facing error output. */
public final class ProtocolConfig {

    private ProtocolConfig() {
    }

    public static final String EMPTY_CELL_TOKEN = ".";

    public static final String BOARD_HEADER = "Board:";

    public static final String COMMANDS_HEADER = "Commands:";

    /** Prefix for the single-line VPL-facing error output, followed by a {@code ParseErrorCode} name. */
    public static final String ERROR_OUTPUT_PREFIX = "ERROR ";

    /** Prefix for the internal, stderr-only I/O failure message (never seen by VPL). */
    public static final String IO_ERROR_PREFIX = "Error: ";
}
