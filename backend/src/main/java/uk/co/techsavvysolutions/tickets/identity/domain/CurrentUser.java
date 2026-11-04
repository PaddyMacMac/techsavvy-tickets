package uk.co.techsavvysolutions.tickets.identity.domain;

import java.util.Set;

public record CurrentUser(String subject, String name, String email, Set<Role> roles) {

    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    public boolean isEngineer() {
        return roles.contains(Role.ENGINEER);
    }

}
