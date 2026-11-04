package uk.co.techsavvysolutions.tickets.ticket.application;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.ticket.domain.PageQuery;
import uk.co.techsavvysolutions.tickets.ticket.domain.Paged;
import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketFilter;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketRepository;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketScope;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;
import uk.co.techsavvysolutions.tickets.ticket.domain.errors.TicketNotFoundException;
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
        var ticketScope = TicketScope.forUser(currentUserProvider.require());
        return ticketRepository.findVisibleById(ticketId, ticketScope)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    public Paged<Ticket> listTickets(TicketStatus status, PageQuery pageQuery) {
        var filter = new TicketFilter(status, TicketScope.forUser(currentUserProvider.require()));
        return ticketRepository.search(filter, pageQuery);
    }
}
