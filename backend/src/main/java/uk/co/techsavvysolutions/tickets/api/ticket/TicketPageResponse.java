package uk.co.techsavvysolutions.tickets.api.ticket;

import java.util.List;

public record TicketPageResponse(List<TicketResponse> items, int page, int size, long totalItems, int totalPages) {}
