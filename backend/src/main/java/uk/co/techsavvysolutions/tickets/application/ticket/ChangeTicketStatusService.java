package uk.co.techsavvysolutions.tickets.application.ticket;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketNotFoundException;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
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
        var ticketScope = currentUserProvider.require().scope();
        var ticket = ticketRepository.findVisibleById(ticketId, ticketScope)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        ticket.changeStatus(newStatus, Instant.now(clock));
        return ticketRepository.save(ticket);
    }
}
