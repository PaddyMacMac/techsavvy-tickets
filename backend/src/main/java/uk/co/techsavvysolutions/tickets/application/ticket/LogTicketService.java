package uk.co.techsavvysolutions.tickets.application.ticket;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles the use case for creating a ticket. */
@Service
@RequiredArgsConstructor
public class LogTicketService {

    private final TicketRepository ticketRepository;
    private final TicketFactory ticketFactory;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public Ticket logTicket(LogTicketCommand command) {
        var requester = currentUserProvider.require();
        return ticketRepository.save(ticketFactory.createTicket(command, requester));
    }
}
