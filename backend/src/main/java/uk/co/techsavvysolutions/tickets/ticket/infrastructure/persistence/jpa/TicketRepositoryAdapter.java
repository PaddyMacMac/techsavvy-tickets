package uk.co.techsavvysolutions.tickets.ticket.infrastructure.persistence.jpa;

import uk.co.techsavvysolutions.tickets.ticket.domain.PageQuery;
import uk.co.techsavvysolutions.tickets.ticket.domain.Paged;
import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketFilter;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketRepository;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketScope;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        return ticketEntityRepository.count(openTicketsInScope(scope)
                .and(TicketSpecifications.respondByBefore(now)));
    }

    @Override
    public Optional<Ticket> findNextResponseDueTicket(TicketScope scope) {
        return ticketEntityRepository.findAll(openTicketsInScope(scope), PageRequest.of(0, 1, SOONEST_DUE)).stream()
                .findFirst()
                .map(ticketEntityMapper::toDomain);
    }

    private static Specification<TicketEntity> openTicketsInScope(TicketScope scope) {
        return TicketSpecifications.inScope(scope).and(TicketSpecifications.hasStatus(TicketStatus.OPEN));
    }
}
