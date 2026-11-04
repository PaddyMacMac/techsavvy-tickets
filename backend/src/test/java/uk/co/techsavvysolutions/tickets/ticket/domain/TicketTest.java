package uk.co.techsavvysolutions.tickets.ticket.domain;

import uk.co.techsavvysolutions.tickets.ticket.domain.errors.InvalidStatusTransitionException;
import static uk.co.techsavvysolutions.tickets.ticket.TicketFixtures.NOW;
import static uk.co.techsavvysolutions.tickets.ticket.TicketFixtures.aTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TicketTest {

    @Test
    void changeStatusUpdatesStatusAndTimestamp() {
        var ticket = aTicket().build();
        var later = NOW.plus(Duration.ofMinutes(5));

        ticket.changeStatus(TicketStatus.IN_PROGRESS, later);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void illegalMoveIsRejectedAndLeavesTicketUnchanged() {
        var ticket = aTicket().build();

        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.RESOLVED, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("OPEN")
                .hasMessageContaining("RESOLVED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }
}
