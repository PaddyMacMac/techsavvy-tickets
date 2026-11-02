package uk.co.techsavvysolutions.tickets.api.identity;

import uk.co.techsavvysolutions.tickets.domain.identity.Role;
import java.util.List;

public record CurrentUserResponse(String subject, String name, String email, List<Role> roles) {}
