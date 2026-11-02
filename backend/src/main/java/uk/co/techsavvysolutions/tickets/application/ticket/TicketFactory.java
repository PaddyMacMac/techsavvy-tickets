package uk.co.techsavvysolutions.tickets.application.ticket;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUser;
import uk.co.techsavvysolutions.tickets.domain.ticket.SlaPolicy;
import uk.co.techsavvysolutions.tickets.domain.ticket.Ticket;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketReferenceGenerator;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Factory: the one place a brand-new ticket is assembled. */
@Component
@RequiredArgsConstructor
public class TicketFactory {

    private final SlaPolicy slaPolicy;
    private final TicketReferenceGenerator ticketReferenceGenerator;
    private final Clock clock;

    public Ticket createTicket(LogTicketCommand logTicketCommand, CurrentUser requesterUser) {
        var createdAt = Instant.now(clock);
        return Ticket.builder()
                .id(UUID.randomUUID())
                .reference(ticketReferenceGenerator.next())
                .title(logTicketCommand.title())
                .description(logTicketCommand.description())
                .category(logTicketCommand.category())
                .priority(logTicketCommand.priority())
                .status(TicketStatus.OPEN)
                .companyName(logTicketCommand.companyName())
                .requesterSubject(requesterUser.subject())
                .requesterName(requesterUser.name())
                .requesterEmail(requesterUser.email())
                .requiredBy(logTicketCommand.requiredBy())
                .respondBy(createdAt.plus(slaPolicy.responseTarget(logTicketCommand.priority())))
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
