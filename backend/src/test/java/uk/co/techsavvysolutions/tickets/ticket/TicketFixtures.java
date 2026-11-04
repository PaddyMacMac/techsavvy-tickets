package uk.co.techsavvysolutions.tickets.ticket;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUser;
import uk.co.techsavvysolutions.tickets.identity.domain.Role;
import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketCategory;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketPriority;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class TicketFixtures {

    public static final Instant NOW = Instant.parse("2026-10-26T09:00:00Z");
    public static final CurrentUser ENGINEER =
            new CurrentUser("eng-1", "Demo Engineer", "engineer@tss.local", Set.of(Role.ENGINEER));
    public static final CurrentUser CLIENT =
            new CurrentUser("client-1", "Demo Client", "client@acme.local", Set.of(Role.CLIENT));

    private TicketFixtures() {
        // prevent instantiation
    }

    public static Ticket.TicketBuilder aTicket() {
        return Ticket.builder().id(UUID.randomUUID()).reference("TSS-" + UUID.randomUUID().toString().substring(0, 8))
                .title("New starter laptop").description("Set up a laptop for Alex starting Monday.")
                .category(TicketCategory.NEW_STARTER).priority(TicketPriority.NORMAL).status(TicketStatus.OPEN)
                .companyName("Acme Bury Ltd").requesterSubject(CLIENT.subject()).requesterName(CLIENT.name())
                .requesterEmail(CLIENT.email()).respondBy(NOW.plusSeconds(8 * 3600)).createdAt(NOW).updatedAt(NOW);
    }
}
