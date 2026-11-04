package uk.co.techsavvysolutions.tickets.ticket.api;

import uk.co.techsavvysolutions.tickets.ticket.domain.TicketCategory;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketPriority;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String reference,
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        String companyName,
        String requesterName,
        String requesterEmail,
        LocalDate requiredBy,
        Instant respondBy,
        Instant createdAt,
        Instant updatedAt,
        List<TicketStatus> allowedTransitions) {}
