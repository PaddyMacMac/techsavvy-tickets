package uk.co.techsavvysolutions.tickets.application.ticket;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.ticket.PageQuery;
import uk.co.techsavvysolutions.tickets.domain.ticket.Paged;
import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketFilter;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketNotFoundException;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryService {

    private final TicketRepository ticketRepository;
    private final CurrentUserProvider currentUserProvider;

    public Ticket getTicketById(UUID ticketId) {
        var ticketScope = currentUserProvider.require().scope();
        return ticketRepository.findVisibleById(ticketId, ticketScope)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    public Paged<Ticket> listTickets(TicketStatus status, PageQuery pageQuery) {
        var filter = new TicketFilter(status, currentUserProvider.require().scope());
        return ticketRepository.search(filter, pageQuery);
    }
}
