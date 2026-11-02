package uk.co.techsavvysolutions.tickets.application.ticket;

import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record TicketSummary(
        Map<TicketStatus, Long> ticketCountsByStatus,
        long overdueAwaitingResponseCount,
        Ticket nextResponseDueTicket) {

    /** Fills in zero for any status with no tickets, so the UI never has to guess. */
    public static TicketSummary fromCounts(
            Map<TicketStatus, Long> statusCounts,
            long overdueAwaitingResponseCount,
            Ticket nextResponseDueTicket) {
        var complete = new EnumMap<TicketStatus, Long>(TicketStatus.class);
        for (var status : TicketStatus.values()) {
            complete.put(status, statusCounts.getOrDefault(status, 0L));
        }
        return new TicketSummary(
                Collections.unmodifiableMap(complete), overdueAwaitingResponseCount, nextResponseDueTicket);
    }
}
