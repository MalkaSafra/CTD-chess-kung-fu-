package com.kungfuchess.rules;

/**
 * Every reason a requested move or jump can be turned down, shared by {@link RuleEngine}'s static
 * legality checks and the engine's real-time checks -- so a single enum, not two independently
 * hand-typed sets of strings, is the source of truth for why a request failed.
 */
public enum MoveRejection {
    /** Not a rejection: the move or jump was accepted. */
    NONE,
    GAME_OVER,
    OUTSIDE_BOARD,
    EMPTY_SOURCE,
    FRIENDLY_DESTINATION,
    ILLEGAL_PIECE_MOVE,
    PIECE_ALREADY_MOVING,
    FRIENDLY_FIRE_AVOIDED
}
