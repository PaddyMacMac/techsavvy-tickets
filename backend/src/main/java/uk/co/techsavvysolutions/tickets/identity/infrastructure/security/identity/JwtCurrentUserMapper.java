package uk.co.techsavvysolutions.tickets.identity.infrastructure.security.identity;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUser;
import uk.co.techsavvysolutions.tickets.identity.domain.Role;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Turns token claims (Entra ID or local dev tokens) into the application's own CurrentUser. */
@Component
public class JwtCurrentUserMapper {

    public CurrentUser fromJwt(Jwt jwt) {
        // 'oid' is Entra's stable per-person id; local dev tokens only have 'sub'.
        var subject = orElse(jwt.getClaimAsString("oid"), Objects.requireNonNullElse(jwt.getSubject(), "unknown"));
        var name = orElse(jwt.getClaimAsString("name"), subject);
        var email = orElse(jwt.getClaimAsString("preferred_username"), orElse(jwt.getClaimAsString("email"), ""));
        var roles = Optional.ofNullable(jwt.getClaimAsStringList("roles")).orElseGet(List::of).stream()
                .map(Role::parse)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
        return new CurrentUser(subject, name, email, roles);
    }

    private static String orElse(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
