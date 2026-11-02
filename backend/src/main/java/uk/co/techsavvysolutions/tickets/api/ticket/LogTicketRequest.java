package uk.co.techsavvysolutions.tickets.api.ticket;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketCategory;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LogTicketRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull TicketCategory category,
        @NotNull TicketPriority priority,
        @NotBlank @Size(max = 120) String companyName,
        @FutureOrPresent LocalDate requiredBy) {}
