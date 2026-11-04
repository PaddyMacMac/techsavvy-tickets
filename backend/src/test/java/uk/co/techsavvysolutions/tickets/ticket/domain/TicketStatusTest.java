package uk.co.techsavvysolutions.tickets.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TicketStatusTest {

    @Test
    void openTicketsCanBeStartedButNotResolvedDirectly() {
        assertThat(TicketStatus.OPEN.canMoveTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.OPEN.canMoveTo(TicketStatus.RESOLVED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void closedIsTerminal(TicketStatus next) {
        assertThat(TicketStatus.CLOSED.canMoveTo(next)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void aStatusNeverMovesToItself(TicketStatus status) {
        assertThat(status.canMoveTo(status)).isFalse();
    }
}
