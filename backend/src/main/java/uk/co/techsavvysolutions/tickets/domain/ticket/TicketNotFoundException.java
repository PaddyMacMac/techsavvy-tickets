package uk.co.techsavvysolutions.tickets.domain.ticket;

import uk.co.techsavvysolutions.tickets.domain.DomainException;
import java.util.UUID;

public final class TicketNotFoundException extends DomainException {

    public TicketNotFoundException(UUID id) {
        super("We couldn't find that request (%s).".formatted(id));
    }
}
