package uk.co.techsavvysolutions.tickets.infrastructure.security.configuration;

import uk.co.techsavvysolutions.tickets.configuration.PortalProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;

/** Production login: trust tokens issued by our Microsoft Entra ID tenant for our app registration. */
@Configuration
@Profile("azure")
class EntraJwtConfiguration {

    @Bean
    JwtDecoder entraJwtDecoder(PortalProperties properties) {
        var entra = Objects.requireNonNull(properties.entra(), "app.entra must be configured");
        Assert.hasText(entra.tenantId(), "ENTRA_TENANT_ID must be set");
        Assert.hasText(entra.clientId(), "ENTRA_CLIENT_ID must be set");

        var issuer = "https://login.microsoftonline.com/%s/v2.0".formatted(entra.tenantId());
        var keys = "https://login.microsoftonline.com/%s/discovery/v2.0/keys".formatted(entra.tenantId());

        var audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                aud -> aud != null && (aud.contains(entra.clientId()) || aud.contains("api://" + entra.clientId())));

        var decoder = NimbusJwtDecoder.withJwkSetUri(keys).build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }
}
