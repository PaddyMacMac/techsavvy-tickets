package uk.co.techsavvysolutions.tickets.application.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.CLIENT;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.ENGINEER;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.aTicket;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketNotFoundException;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketScope;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;

@ExtendWith(MockitoExtension.class)
class ChangeTicketStatusServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserProvider currentUserProvider;

    private ChangeTicketStatusService service;

    @BeforeEach
    void setUp() {
        service = new ChangeTicketStatusService(
                ticketRepository,
                currentUserProvider,
                Clock.fixed(Instant.parse("2026-10-26T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void changesStatusAndSavesTicket() {
        var ticket = aTicket().build();
        when(currentUserProvider.require()).thenReturn(ENGINEER);
        when(ticketRepository.findVisibleById(ticket.getId(), new TicketScope.AllTickets()))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        var updatedTicket = service.changeTicketStatus(ticket.getId(), TicketStatus.IN_PROGRESS);

        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void hidesTicketsOutsideTheCurrentUsersScope() {
        var ticketId = UUID.randomUUID();
        when(currentUserProvider.require()).thenReturn(CLIENT);
        when(ticketRepository.findVisibleById(ticketId, new TicketScope.OwnTickets(CLIENT.subject())))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeTicketStatus(ticketId, TicketStatus.CLOSED))
                .isInstanceOf(TicketNotFoundException.class);
    }
}
