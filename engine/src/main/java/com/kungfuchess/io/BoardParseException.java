package com.kungfuchess.io;

public class BoardParseException extends RuntimeException {

    private final ParseErrorCode code;

    public BoardParseException(ParseErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ParseErrorCode getCode() {
        return code;
    }
}