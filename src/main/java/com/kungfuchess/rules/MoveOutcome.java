package com.kungfuchess.rules;

public final class MoveOutcome {

    public static final MoveOutcome ACCEPTED = new MoveOutcome(true, MoveRejection.NONE);

    private final boolean accepted;
    private final MoveRejection reason;

    public MoveOutcome(boolean accepted, MoveRejection reason) {
        this.accepted = accepted;
        this.reason = reason;
    }

    public static MoveOutcome rejected(MoveRejection reason) {
        return new MoveOutcome(false, reason);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public MoveRejection getReason() {
        return reason;
    }
}
