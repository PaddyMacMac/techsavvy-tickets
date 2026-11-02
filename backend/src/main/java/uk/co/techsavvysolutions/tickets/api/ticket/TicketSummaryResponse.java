package uk.co.techsavvysolutions.tickets.api.ticket;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.util.Map;

public record TicketSummaryResponse(
        Map<TicketStatus, Long> byStatus, long awaitingResponseOverdue, TicketResponse nextResponseDue) {}
