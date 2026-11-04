package uk.co.techsavvysolutions.tickets.ticket.domain.errors;

import uk.co.techsavvysolutions.tickets.shared.DomainException;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;
public final class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(String reference, TicketStatus from, TicketStatus to) {
        super("%s can't move from %s to %s.".formatted(reference, from, to));
    }
}
