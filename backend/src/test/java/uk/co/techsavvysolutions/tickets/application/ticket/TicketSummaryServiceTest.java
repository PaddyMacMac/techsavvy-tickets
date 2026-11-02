package uk.co.techsavvysolutions.tickets.application.ticket;

import static uk.co.techsavvysolutions.tickets.TicketFixtures.CLIENT;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.NOW;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.aTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketScope;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketSummaryServiceTest {

    @Mock TicketRepository tickets;

    @Test
    void summaryCombinesRepositoryMetricsForTheCurrentUsersScope() {
        CurrentUserProvider currentUser = () -> CLIENT;
        var service = new TicketSummaryService(tickets, currentUser, Clock.fixed(NOW, ZoneOffset.UTC));
        var scope = new TicketScope.OwnTickets(CLIENT.subject());
        var next = aTicket().build();
        when(tickets.countByStatus(scope)).thenReturn(Map.of(TicketStatus.OPEN, 3L));
        when(tickets.countOverdueAwaitingResponse(scope, NOW)).thenReturn(2L);
        when(tickets.findNextResponseDueTicket(scope)).thenReturn(Optional.of(next));

        var summary = service.getSummary();

        assertThat(summary.ticketCountsByStatus())
                .containsEntry(TicketStatus.OPEN, 3L)
                .containsEntry(TicketStatus.CLOSED, 0L);
        assertThat(summary.overdueAwaitingResponseCount()).isEqualTo(2L);
        assertThat(summary.nextResponseDueTicket()).isSameAs(next);
    }
}
