package uk.co.techsavvysolutions.tickets.identity.infrastructure.security.configuration;

import uk.co.techsavvysolutions.tickets.identity.configuration.PortalProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/** LOCAL ONLY: signs and verifies tokens with a shared secret so you can develop without an Azure tenant. */
@Configuration
@Profile("local")
class LocalJwtConfiguration {

    @Bean
    SecretKey devJwtKey(PortalProperties properties) {
        var dev = Objects.requireNonNull(properties.dev(), "app.dev must be configured for the local profile");
        return new SecretKeySpec(dev.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder devJwtDecoder(SecretKey devJwtKey) {
        return NimbusJwtDecoder.withSecretKey(devJwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder devJwtEncoder(SecretKey devJwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(devJwtKey));
    }

    @Bean
    PasswordEncoder localPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
