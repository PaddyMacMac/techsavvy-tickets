package uk.co.techsavvysolutions.tickets.domain.ticket;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Port: the domain says what it needs, infrastructure decides how. */
public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findVisibleById(UUID id, TicketScope scope);

    Paged<Ticket> search(TicketFilter filter, PageQuery page);

    Map<TicketStatus, Long> countByStatus(TicketScope scope);

    long countOverdueAwaitingResponse(TicketScope scope, Instant now);

    Optional<Ticket> findNextResponseDueTicket(TicketScope scope);
}
