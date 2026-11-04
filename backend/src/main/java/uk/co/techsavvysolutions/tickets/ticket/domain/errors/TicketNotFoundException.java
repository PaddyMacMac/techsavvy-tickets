package uk.co.techsavvysolutions.tickets.ticket.domain.errors;

import uk.co.techsavvysolutions.tickets.shared.DomainException;
import java.util.UUID;

public final class TicketNotFoundException extends DomainException {

    public TicketNotFoundException(UUID id) {
        super("We couldn't find that request (%s).".formatted(id));
    }
}
