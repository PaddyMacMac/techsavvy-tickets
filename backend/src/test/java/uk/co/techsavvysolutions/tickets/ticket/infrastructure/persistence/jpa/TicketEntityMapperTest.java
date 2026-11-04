package uk.co.techsavvysolutions.tickets.ticket.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.techsavvysolutions.tickets.ticket.TicketFixtures.aTicket;

import jakarta.persistence.Entity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;

class TicketEntityMapperTest {

    private final TicketEntityMapper mapper = new TicketEntityMapper();

    @Test
    void mapsTicketToPersistenceAndBackWithoutLosingDomainData() {
        var ticket = aTicket().build();
        var company = CompanyEntity.builder()
                .id(UUID.randomUUID())
                .name(ticket.getCompanyName())
                .build();
        var requester = RequesterEntity.builder()
                .subject(ticket.getRequesterSubject())
                .name(ticket.getRequesterName())
                .email(ticket.getRequesterEmail())
                .build();

        var mapped = mapper.toDomain(mapper.toEntity(ticket, company, requester));

        assertThat(mapped).usingRecursiveComparison().isEqualTo(ticket);
    }

    @Test
    void mapsOptimisticLockRevisionWithoutExposingJpaVersionInDomain() {
        var ticket = aTicket().revision(7L).build();
        var company = CompanyEntity.builder()
                .id(UUID.randomUUID())
                .name(ticket.getCompanyName())
                .build();
        var requester = RequesterEntity.builder()
                .subject(ticket.getRequesterSubject())
                .name(ticket.getRequesterName())
                .email(ticket.getRequesterEmail())
                .build();

        var entity = mapper.toEntity(ticket, company, requester);
        var mapped = mapper.toDomain(entity);

        assertThat(entity.getVersion()).isEqualTo(7L);
        assertThat(mapped.getRevision()).isEqualTo(7L);
    }

    @Test
    void domainAggregateDoesNotDependOnJpaAnnotations() {
        assertThat(Ticket.class.getAnnotation(Entity.class)).isNull();
    }
}
