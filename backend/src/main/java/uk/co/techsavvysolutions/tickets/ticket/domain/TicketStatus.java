package uk.co.techsavvysolutions.tickets.ticket.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of a request. The allowed moves live here, next to the states they belong to. */
public enum TicketStatus {
    OPEN, IN_PROGRESS, WAITING_ON_CLIENT, RESOLVED, CLOSED;

    public Set<TicketStatus> allowedNext() {
        return switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS, CLOSED);
            case IN_PROGRESS -> EnumSet.of(OPEN, WAITING_ON_CLIENT, RESOLVED);
            case WAITING_ON_CLIENT -> EnumSet.of(IN_PROGRESS, CLOSED);
            case RESOLVED -> EnumSet.of(IN_PROGRESS, CLOSED);
            case CLOSED -> EnumSet.noneOf(TicketStatus.class);
        };
    }

    public boolean canMoveTo(TicketStatus next) {
        return allowedNext().contains(next);
    }
}
