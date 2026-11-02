package uk.co.techsavvysolutions.tickets.infrastructure.persistence.jpa;

import uk.co.techsavvysolutions.tickets.domain.ticket.PageQuery;
import uk.co.techsavvysolutions.tickets.domain.ticket.Paged;
import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketFilter;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketScope;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Adapter: implements the domain's repository port with Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class TicketRepositoryAdapter implements TicketRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Sort SOONEST_DUE = Sort.by(Sort.Direction.ASC, "respondBy");

    private final SpringDataTicketRepository ticketEntityRepository;
    private final TicketPartySynchronizer ticketPartySynchronizer;
    private final TicketEntityMapper ticketEntityMapper;

    @Override
    public Ticket save(Ticket ticket) {
        var parties = ticketPartySynchronizer.forTicket(ticket);
        var entity = ticketEntityMapper.toEntity(ticket, parties.company(), parties.requester());
        return ticketEntityMapper.toDomain(ticketEntityRepository.save(entity));
    }

    @Override
    public Optional<Ticket> findVisibleById(UUID id, TicketScope scope) {
        return ticketEntityRepository
                .findOne(TicketSpecifications.withId(id).and(TicketSpecifications.inScope(scope)))
                .map(ticketEntityMapper::toDomain);
    }

    @Override
    public Paged<Ticket> search(TicketFilter filter, PageQuery page) {
        var resultPage = ticketEntityRepository.findAll(
                TicketSpecifications.matching(filter),
                PageRequest.of(page.page(), page.size(), NEWEST_FIRST));
        var tickets = resultPage.getContent().stream().map(ticketEntityMapper::toDomain).toList();
        return new Paged<>(tickets, page.page(), page.size(), resultPage.getTotalElements());
    }

    @Override
    public Map<TicketStatus, Long> countByStatus(TicketScope scope) {
        var counts = switch (scope) {
            case TicketScope.AllTickets _ -> ticketEntityRepository.countGroupedByStatus();
            case TicketScope.OwnTickets(var subject) -> ticketEntityRepository.countGroupedByStatusForRequester(subject);
        };
        return counts.stream()
                .collect(Collectors.toMap(SpringDataTicketRepository.StatusCount::getStatus, SpringDataTicketRepository.StatusCount::getTotal));
    }

    @Override
    public long countOverdueAwaitingResponse(TicketScope scope, Instant now) {
        return ticketEntityRepository.count(TicketSpecifications.inScope(scope)
                .and(TicketSpecifications.hasStatus(TicketStatus.OPEN))
                .and(TicketSpecifications.respondByBefore(now)));
    }

    @Override
    public Optional<Ticket> findNextResponseDueTicket(TicketScope scope) {
        var spec = TicketSpecifications.inScope(scope).and(TicketSpecifications.hasStatus(TicketStatus.OPEN));
        return ticketEntityRepository.findAll(spec, PageRequest.of(0, 1, SOONEST_DUE)).stream()
                .findFirst()
                .map(ticketEntityMapper::toDomain);
    }
}
