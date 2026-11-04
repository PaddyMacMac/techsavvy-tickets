package uk.co.techsavvysolutions.tickets.ticket.application;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketRepository;
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
