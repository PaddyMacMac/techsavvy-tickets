package uk.co.techsavvysolutions.tickets.infrastructure.security.configuration;

import uk.co.techsavvysolutions.tickets.configuration.PortalProperties;
import uk.co.techsavvysolutions.tickets.infrastructure.security.identity.AuthenticatedUserContextFilter;
import uk.co.techsavvysolutions.tickets.infrastructure.security.identity.JwtCurrentUserMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The API is a stateless OAuth2 resource server: every call carries a bearer token, so there are no cookies
 * to forge and CSRF protection is switched off on purpose. Who may do what is decided in one place, below.
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            JwtAuthenticationConverter jwtConverter,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource,
            JwtCurrentUserMapper userMapper)
            throws Exception {
        return http.cors(cors -> cors.configurationSource(corsSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**", "/api/auth/dev-login")
                        .permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/status")
                        .hasRole("ENGINEER")
                        .requestMatchers("/api/**")
                        .hasAnyRole("ENGINEER", "CLIENT")
                        .anyRequest()
                        .denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
                .addFilterAfter(new AuthenticatedUserContextFilter(userMapper), BearerTokenAuthenticationFilter.class)
                .build();
    }

    /** Reads the token's "roles" claim (Entra app roles) and exposes each as ROLE_<VALUE>. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(PortalProperties properties) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
