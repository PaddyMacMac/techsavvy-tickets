package uk.co.techsavvysolutions.tickets.ticket.application;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketRepository;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketScope;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;
import uk.co.techsavvysolutions.tickets.ticket.domain.errors.TicketNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles status changes for tickets visible to the current user. */
@Service
@RequiredArgsConstructor
public class ChangeTicketStatusService {

    private final TicketRepository ticketRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    @Transactional
    public Ticket changeTicketStatus(UUID ticketId, TicketStatus newStatus) {
        var ticketScope = TicketScope.forUser(currentUserProvider.require());
        var ticket = ticketRepository.findVisibleById(ticketId, ticketScope)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        ticket.changeStatus(newStatus, Instant.now(clock));
        return ticketRepository.save(ticket);
    }
}
