package uk.co.techsavvysolutions.tickets.ticket.api;

import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;
import java.util.Map;

public record TicketSummaryResponse(
        Map<TicketStatus, Long> byStatus, long awaitingResponseOverdue, TicketResponse nextResponseDue) {}
