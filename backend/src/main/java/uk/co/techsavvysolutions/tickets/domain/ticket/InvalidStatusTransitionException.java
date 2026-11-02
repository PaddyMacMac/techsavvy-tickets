package uk.co.techsavvysolutions.tickets.domain.ticket;

import uk.co.techsavvysolutions.tickets.domain.DomainException;
public final class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(String reference, TicketStatus from, TicketStatus to) {
        super("%s can't move from %s to %s.".formatted(reference, from, to));
    }
}
