package uk.co.techsavvysolutions.tickets.ticket.infrastructure.persistence.sequence;

import uk.co.techsavvysolutions.tickets.ticket.domain.TicketReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PostgresTicketReferenceGenerator implements TicketReferenceGenerator {

    private final JdbcClient jdbc;

    @Override
    public String next() {
        var number = jdbc.sql("select nextval('ticket_reference_seq')").query(Long.class).single();
        return "TSS-%06d".formatted(number);
    }
}
