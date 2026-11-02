package uk.co.techsavvysolutions.tickets.application.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.CLIENT;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.NOW;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.ticket.DefaultSlaPolicy;
import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketCategory;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketPriority;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketReferenceGenerator;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;

@ExtendWith(MockitoExtension.class)
class LogTicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock TicketReferenceGenerator ticketReferenceGenerator;
    @Mock CurrentUserProvider currentUserProvider;

    private LogTicketService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var ticketFactory = new TicketFactory(new DefaultSlaPolicy(), ticketReferenceGenerator, clock);
        service = new LogTicketService(ticketRepository, ticketFactory, currentUserProvider);
    }

    @Test
    void createsAndSavesTicketWithResponseDeadline() {
        when(currentUserProvider.require()).thenReturn(CLIENT);
        when(ticketReferenceGenerator.next()).thenReturn("TSS-001000");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        var command = new LogTicketCommand(
                "Printer offline", "Front office printer will not connect.", TicketCategory.HARDWARE,
                TicketPriority.URGENT, "Acme Bury Ltd", null);

        var ticket = service.logTicket(command);

        assertThat(ticket.getReference()).isEqualTo("TSS-001000");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getRequesterSubject()).isEqualTo(CLIENT.subject());
        assertThat(ticket.getRespondBy()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        verify(ticketRepository).save(ticket);
    }
}
