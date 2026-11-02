package uk.co.techsavvysolutions.tickets.infrastructure.persistence.sequence;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketReferenceGenerator;
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
