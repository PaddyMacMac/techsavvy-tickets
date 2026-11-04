package uk.co.techsavvysolutions.tickets.ticket.application;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketRepository;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketScope;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the dashboard summary from the ticket repository's independent read operations. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketSummaryService {

    private final TicketRepository ticketRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public TicketSummary getSummary() {
        var ticketScope = TicketScope.forUser(currentUserProvider.require());
        var summaryTime = Instant.now(clock);
        var statusCounts = ticketRepository.countByStatus(ticketScope);
        var overdueAwaitingResponseCount = ticketRepository.countOverdueAwaitingResponse(ticketScope, summaryTime);
        var nextResponseDueTicket = ticketRepository.findNextResponseDueTicket(ticketScope);
        return TicketSummary.fromCounts(
                statusCounts, overdueAwaitingResponseCount, nextResponseDueTicket.orElse(null));
    }
}
