package uk.co.techsavvysolutions.tickets.application.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.CLIENT;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.aTicket;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.ticket.PageQuery;
import uk.co.techsavvysolutions.tickets.domain.ticket.Paged;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketFilter;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketNotFoundException;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketScope;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceTest {

    @Mock TicketRepository ticketRepository;

    private final CurrentUserProvider currentUserProvider = () -> CLIENT;

    @Test
    void getsTicketWithinCurrentUsersScope() {
        var ticket = aTicket().build();
        var scope = new TicketScope.OwnTickets(CLIENT.subject());
        when(ticketRepository.findVisibleById(ticket.getId(), scope)).thenReturn(Optional.of(ticket));

        var result = new TicketQueryService(ticketRepository, currentUserProvider).getTicketById(ticket.getId());

        assertThat(result).isSameAs(ticket);
    }

    @Test
    void reportsMissingTicketWhenItIsOutsideCurrentUsersScope() {
        var ticketId = UUID.randomUUID();
        when(ticketRepository.findVisibleById(ticketId, new TicketScope.OwnTickets(CLIENT.subject())))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new TicketQueryService(ticketRepository, currentUserProvider).getTicketById(ticketId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void listsTicketsWithCurrentUserScopeAndRequestedPage() {
        var pageQuery = new PageQuery(2, 10);
        var expectedPage = new Paged<>(List.of(aTicket().build()), 2, 10, 21);
        when(ticketRepository.search(new TicketFilter(TicketStatus.OPEN,
                new TicketScope.OwnTickets(CLIENT.subject())), pageQuery)).thenReturn(expectedPage);

        var result = new TicketQueryService(ticketRepository, currentUserProvider)
                .listTickets(TicketStatus.OPEN, pageQuery);

        assertThat(result).isSameAs(expectedPage);
        verify(ticketRepository).search(
                new TicketFilter(TicketStatus.OPEN, new TicketScope.OwnTickets(CLIENT.subject())), pageQuery);
    }
}
