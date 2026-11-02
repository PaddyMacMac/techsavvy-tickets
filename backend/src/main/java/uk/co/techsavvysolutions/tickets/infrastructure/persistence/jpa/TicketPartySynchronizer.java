package uk.co.techsavvysolutions.tickets.infrastructure.persistence.jpa;

import java.util.UUID;
import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Keeps requester profiles and companies current before tickets reference them. */
@Component
@RequiredArgsConstructor
class TicketPartySynchronizer {

    private final JdbcClient jdbc;
    private final SpringDataCompanyRepository companyRepository;
    private final SpringDataRequesterRepository requesterRepository;

    TicketParties forTicket(Ticket ticket) {
        if (ticket.getRevision() == null) {
            return new TicketParties(
                    synchronizeCompany(ticket.getCompanyName()),
                    synchronizeRequester(
                            ticket.getRequesterSubject(), ticket.getRequesterName(), ticket.getRequesterEmail()));
        }

        var company = companyRepository.findByName(ticket.getCompanyName())
                .orElseThrow(() -> new IllegalStateException("Company for an existing ticket is missing."));
        return new TicketParties(company, requesterRepository.getReferenceById(ticket.getRequesterSubject()));
    }

    private CompanyEntity synchronizeCompany(String name) {
        var id = jdbc.sql("""
                        insert into companies (id, name)
                        values (:id, :name)
                        on conflict (name) do update set name = excluded.name
                        returning id
                        """)
                .param("id", UUID.randomUUID())
                .param("name", name)
                .query(UUID.class)
                .single();
        return companyRepository.getReferenceById(id);
    }

    private RequesterEntity synchronizeRequester(String subject, String name, String email) {
        var persistedSubject = jdbc.sql("""
                        insert into requesters (subject, name, email)
                        values (:subject, :name, :email)
                        on conflict (subject) do update
                        set name = excluded.name, email = excluded.email
                        returning subject
                        """)
                .param("subject", subject)
                .param("name", name)
                .param("email", email)
                .query(String.class)
                .single();
        var requester = requesterRepository.getReferenceById(persistedSubject);
        requester.updateProfile(name, email);
        return requester;
    }

    record TicketParties(CompanyEntity company, RequesterEntity requester) {}
}
