package uk.co.techsavvysolutions.tickets.api.ticket;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTicketStatusRequest(@NotNull TicketStatus status) {}
