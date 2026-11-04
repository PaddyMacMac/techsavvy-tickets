package uk.co.techsavvysolutions.tickets.ticket.api;

import uk.co.techsavvysolutions.tickets.ticket.application.TicketSummary;
import uk.co.techsavvysolutions.tickets.ticket.domain.Paged;
import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;
import org.springframework.stereotype.Component;

/** Converts ticket read models into the stable HTTP response contract. */
@Component
class TicketResponseMapper {

    TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getCompanyName(),
                ticket.getRequesterName(),
                ticket.getRequesterEmail(),
                ticket.getRequiredBy(),
                ticket.getRespondBy(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getStatus().allowedNext().stream().toList());
    }

    TicketPageResponse toPageResponse(Paged<Ticket> page) {
        var responsePage = page.map(this::toResponse);
        return new TicketPageResponse(
                responsePage.items(),
                responsePage.page(),
                responsePage.size(),
                responsePage.totalItems(),
                responsePage.totalPages());
    }

    TicketSummaryResponse toSummaryResponse(TicketSummary summary) {
        var nextResponseDueTicket = summary.nextResponseDueTicket();
        return new TicketSummaryResponse(
                summary.ticketCountsByStatus(),
                summary.overdueAwaitingResponseCount(),
                nextResponseDueTicket == null ? null : toResponse(nextResponseDueTicket));
    }
}
