package uk.co.techsavvysolutions.tickets.ticket.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.techsavvysolutions.tickets.ticket.TicketFixtures.aTicket;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.simple.JdbcClient;

@ExtendWith(MockitoExtension.class)
class TicketPartySynchronizerTest {

    @Mock JdbcClient jdbc;
    @Mock SpringDataCompanyRepository companyRepository;
    @Mock SpringDataRequesterRepository requesterRepository;

    @Test
    void updatingATicketReusesItsNormalizedPartiesWithoutOverwritingTheirProfiles() {
        var ticket = aTicket().revision(4L).build();
        var company = CompanyEntity.builder().id(UUID.randomUUID()).name(ticket.getCompanyName()).build();
        var requester = RequesterEntity.builder()
                .subject(ticket.getRequesterSubject())
                .name("Latest Profile Name")
                .email("latest@example.test")
                .build();
        when(companyRepository.findByName(ticket.getCompanyName())).thenReturn(Optional.of(company));
        when(requesterRepository.getReferenceById(ticket.getRequesterSubject())).thenReturn(requester);

        var parties = new TicketPartySynchronizer(jdbc, companyRepository, requesterRepository).forTicket(ticket);

        assertThat(parties.company()).isSameAs(company);
        assertThat(parties.requester()).isSameAs(requester);
        assertThat(parties.requester().getName()).isEqualTo("Latest Profile Name");
        verify(companyRepository).findByName(ticket.getCompanyName());
        verify(requesterRepository).getReferenceById(ticket.getRequesterSubject());
        verifyNoInteractions(jdbc);
    }
}
