package uk.co.techsavvysolutions.tickets.infrastructure.security.local;

import uk.co.techsavvysolutions.tickets.application.identity.IssuedAccessToken;
import uk.co.techsavvysolutions.tickets.application.identity.LocalLoginUseCase;
import uk.co.techsavvysolutions.tickets.configuration.PortalProperties;
import uk.co.techsavvysolutions.tickets.domain.identity.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Validates local demo credentials and signs a short-lived JWT for the matched user. */
@Service
@Profile("local")
class LocalLoginService implements LocalLoginUseCase {

    private final PortalProperties.Dev localLoginProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    LocalLoginService(
            PortalProperties portalProperties,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            Clock clock) {
        this.localLoginProperties = Objects.requireNonNull(
                portalProperties.dev(), "app.dev must be configured for the local profile");
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    @Override
    public Optional<IssuedAccessToken> authenticate(String email, String password) {
        var matchingUser = localLoginProperties.users().stream()
                .filter(user -> user.email().equalsIgnoreCase(email))
                .filter(user -> passwordEncoder.matches(password, user.password()))
                .findFirst();
        return matchingUser.map(this::issueAccessToken);
    }

    private IssuedAccessToken issueAccessToken(PortalProperties.Dev.DevUser user) {
        var issuedAt = Instant.now(clock);
        var expiresAt = issuedAt.plus(localLoginProperties.tokenTtl());
        var claims = JwtClaimsSet.builder()
                .subject(user.email())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("name", user.name())
                .claim("email", user.email())
                .claim("roles", user.roles().stream().map(Role::name).toList())
                .build();
        var signedHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        var tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(signedHeader, claims)).getTokenValue();
        return new IssuedAccessToken(tokenValue, localLoginProperties.tokenTtl().toSeconds());
    }
}
