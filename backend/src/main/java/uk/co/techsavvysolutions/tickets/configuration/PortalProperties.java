package uk.co.techsavvysolutions.tickets.configuration;

import uk.co.techsavvysolutions.tickets.domain.identity.Role;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record PortalProperties(Cors cors, Entra entra, Dev dev) {

    public record Cors(List<String> allowedOrigins) {}

    public record Entra(String tenantId, String clientId) {}

    public record Dev(String jwtSecret, Duration tokenTtl, List<DevUser> users) {

        public record DevUser(String email, String password, String name, Set<Role> roles) {}
    }
}
