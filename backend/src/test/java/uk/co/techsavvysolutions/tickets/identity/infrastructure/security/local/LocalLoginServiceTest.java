package uk.co.techsavvysolutions.tickets.identity.infrastructure.security.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import uk.co.techsavvysolutions.tickets.identity.configuration.PortalProperties;
import uk.co.techsavvysolutions.tickets.identity.domain.Role;

class LocalLoginServiceTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-10-26T09:00:00Z");
    private static final String EMAIL = "engineer@tss.local";
    private static final String PASSWORD = "Engineer123!";

    @Test
    void issuesSignedTokenForMatchingDemoCredentials() {
        var service = createService(parameters -> signedJwt("signed-token"));

        var result = service.authenticate(EMAIL, PASSWORD);

        assertThat(result).hasValueSatisfying(token -> {
            assertThat(token.accessToken()).isEqualTo("signed-token");
            assertThat(token.expiresInSeconds()).isEqualTo(Duration.ofHours(8).toSeconds());
        });
    }

    @Test
    void rejectsIncorrectPassword() {
        var service = createService(parameters -> signedJwt("should-not-be-used"));

        assertThat(service.authenticate(EMAIL, "incorrect")).isEmpty();
    }

    private static LocalLoginService createService(JwtEncoder tokenEncoder) {
        var demoUser = new PortalProperties.Dev.DevUser(
                EMAIL, "{noop}" + PASSWORD, "Demo Engineer", Set.of(Role.ENGINEER));
        var devSettings = new PortalProperties.Dev("local-secret", Duration.ofHours(8), List.of(demoUser));
        var properties = new PortalProperties(new PortalProperties.Cors(List.of()), null, devSettings);
        return new LocalLoginService(
                properties,
                PasswordEncoderFactories.createDelegatingPasswordEncoder(),
                tokenEncoder,
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
    }

    private static Jwt signedJwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .subject(EMAIL)
                .issuedAt(ISSUED_AT)
                .expiresAt(ISSUED_AT.plus(Duration.ofHours(8)))
                .build();
    }
}
