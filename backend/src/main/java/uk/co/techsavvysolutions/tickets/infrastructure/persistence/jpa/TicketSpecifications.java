package uk.co.techsavvysolutions.tickets.infrastructure.persistence.jpa;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketFilter;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketScope;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

final class TicketSpecifications {

    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String REQUESTER = "requester";
    private static final String REQUESTER_SUBJECT = "subject";
    private static final String RESPOND_BY = "respondBy";

    private TicketSpecifications() {}

    static Specification<TicketEntity> matching(TicketFilter filter) {
        var scoped = inScope(filter.scope());
        var status = filter.status();
        return status == null ? scoped : scoped.and(hasStatus(status));
    }

    static Specification<TicketEntity> inScope(TicketScope scope) {
        return switch (scope) {
            case TicketScope.AllTickets _ -> unrestricted();
            case TicketScope.OwnTickets(var subject) ->
                (root, query, cb) -> cb.equal(root.get(REQUESTER).get(REQUESTER_SUBJECT), subject);
        };
    }

    static Specification<TicketEntity> withId(UUID id) {
        return (root, query, cb) -> cb.equal(root.get(ID), id);
    }

    static Specification<TicketEntity> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get(STATUS), status);
    }

    static Specification<TicketEntity> respondByBefore(Instant moment) {
        return (root, query, cb) -> cb.lessThan(root.<Instant>get(RESPOND_BY), moment);
    }

    private static Specification<TicketEntity> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }
}
