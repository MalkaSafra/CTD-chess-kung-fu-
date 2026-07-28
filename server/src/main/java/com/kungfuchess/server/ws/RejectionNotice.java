package com.kungfuchess.server.ws;

/**
 * Sent privately back to whichever session made a rejected request -- never broadcast, unlike
 * {@code TransientEvent}. A rejection is specific to the requester (a mis-click, a piece still
 * resting, etc.); every other connected player hearing a "denied" sound for someone else's
 * mistake would be wrong, and would leak that another player just attempted something.
 */
public record RejectionNotice(String reason) {
}
