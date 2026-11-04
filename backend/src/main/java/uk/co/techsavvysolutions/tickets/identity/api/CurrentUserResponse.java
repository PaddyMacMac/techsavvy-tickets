package uk.co.techsavvysolutions.tickets.identity.api;

import uk.co.techsavvysolutions.tickets.identity.domain.Role;
import java.util.List;

public record CurrentUserResponse(String subject, String name, String email, List<Role> roles) {}
