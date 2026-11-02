package uk.co.techsavvysolutions.tickets.application.ticket;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketCategory;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketPriority;
import java.time.LocalDate;

public record LogTicketCommand(
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        String companyName,
        LocalDate requiredBy) {}
