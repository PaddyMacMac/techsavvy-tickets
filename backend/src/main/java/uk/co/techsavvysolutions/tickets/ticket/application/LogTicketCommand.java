package uk.co.techsavvysolutions.tickets.ticket.application;

import uk.co.techsavvysolutions.tickets.ticket.domain.TicketCategory;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketPriority;
import java.time.LocalDate;

public record LogTicketCommand(
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        String companyName,
        LocalDate requiredBy) {}
