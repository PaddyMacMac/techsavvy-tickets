package uk.co.techsavvysolutions.tickets.ticket.api;

import java.util.List;

public record TicketPageResponse(List<TicketResponse> items, int page, int size, long totalItems, int totalPages) {}
