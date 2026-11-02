package uk.co.techsavvysolutions.tickets.domain.identity;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketScope;
import java.util.Set;

public record CurrentUser(String subject, String name, String email, Set<Role> roles) {

    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    public boolean isEngineer() {
        return roles.contains(Role.ENGINEER);
    }

    public TicketScope scope() {
        return isEngineer() ? new TicketScope.AllTickets() : new TicketScope.OwnTickets(subject);
    }
}
