package uk.co.techsavvysolutions.tickets.infrastructure.persistence.jpa;

import static uk.co.techsavvysolutions.tickets.TicketFixtures.CLIENT;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.NOW;
import static uk.co.techsavvysolutions.tickets.TicketFixtures.aTicket;
import static org.assertj.core.api.Assertions.assertThat;

import uk.co.techsavvysolutions.tickets.domain.ticket.PageQuery;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketFilter;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketRepository;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketScope;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Needs Docker running. Starts a throwaway PostgreSQL, runs the real Flyway scripts, then checks the mapping. */
@SpringBootTest
@Transactional
@Import(TicketRepositoryAdapterIntegrationTest.Postgres.class)
class TicketRepositoryAdapterIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Postgres {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:17-alpine");
        }
    }

    @Autowired TicketRepository tickets;
    @Autowired SpringDataCompanyRepository companies;
    @Autowired SpringDataRequesterRepository requesters;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void reusesNormalizedCompanyAndRequesterRecords() {
        var originalTicket = aTicket().build();
        var updatedRequesterTicket = aTicket()
                .requesterSubject(originalTicket.getRequesterSubject())
                .requesterName("Updated Client")
                .requesterEmail("updated@acme.local")
                .companyName(originalTicket.getCompanyName())
                .build();

        tickets.save(originalTicket);
        tickets.save(updatedRequesterTicket);

        assertThat(companies.count()).isEqualTo(1);
        assertThat(requesters.count()).isEqualTo(1);
        var reloadedTicket = tickets.findVisibleById(originalTicket.getId(), new TicketScope.AllTickets())
                .orElseThrow();
        assertThat(reloadedTicket.getRequesterName()).isEqualTo("Updated Client");
        assertThat(reloadedTicket.getRequesterEmail()).isEqualTo("updated@acme.local");

        reloadedTicket.changeStatus(TicketStatus.IN_PROGRESS, NOW);
        tickets.save(reloadedTicket);
        var savedStatusChange = tickets.findVisibleById(originalTicket.getId(), new TicketScope.AllTickets())
                .orElseThrow();
        assertThat(savedStatusChange.getRequesterName()).isEqualTo("Updated Client");
    }

    @Test
    void clientsOnlySeeTheirOwnTickets() {
        tickets.save(aTicket().build());
        tickets.save(aTicket().requesterSubject("someone-else").build());

        var own = tickets.search(new TicketFilter(null, new TicketScope.OwnTickets(CLIENT.subject())), new PageQuery(0, 10));
        var all = tickets.search(new TicketFilter(null, new TicketScope.AllTickets()), new PageQuery(0, 10));

        assertThat(own.items()).hasSize(1);
        assertThat(all.items()).hasSize(2);
    }

    @Test
    void filtersByStatusAndCountsPerStatus() {
        tickets.save(aTicket().build());
        tickets.save(aTicket().status(TicketStatus.CLOSED).build());

        var open = tickets.search(new TicketFilter(TicketStatus.OPEN, new TicketScope.AllTickets()), new PageQuery(0, 10));
        var counts = tickets.countByStatus(new TicketScope.AllTickets());

        assertThat(open.items()).hasSize(1);
        assertThat(counts).containsEntry(TicketStatus.OPEN, 1L).containsEntry(TicketStatus.CLOSED, 1L);
    }

    @Test
    void findsOverdueAndNextDueOpenTickets() {
        var soon = tickets.save(aTicket().respondBy(NOW.plusSeconds(60)).build());
        tickets.save(aTicket().respondBy(NOW.plusSeconds(3600)).build());
        tickets.save(aTicket().respondBy(NOW.minusSeconds(60)).status(TicketStatus.CLOSED).build());

        var scope = new TicketScope.AllTickets();

        assertThat(tickets.countOverdueAwaitingResponse(scope, NOW.plusSeconds(120))).isEqualTo(1);
        assertThat(tickets.findNextResponseDueTicket(scope)).get().extracting("id").isEqualTo(soon.getId());
    }

    @Test
    void overdueAndNextDueQueriesRespectRequesterScope() {
        var ownSoon = tickets.save(aTicket().requesterSubject(CLIENT.subject()).respondBy(NOW.plusSeconds(60)).build());
        tickets.save(aTicket().requesterSubject("someone-else").respondBy(NOW.minusSeconds(60)).build());

        var scope = new TicketScope.OwnTickets(CLIENT.subject());

        assertThat(tickets.countOverdueAwaitingResponse(scope, NOW)).isZero();
        assertThat(tickets.findNextResponseDueTicket(scope)).get().extracting("id").isEqualTo(ownSoon.getId());
    }
}
