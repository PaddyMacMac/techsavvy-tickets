package uk.co.techsavvysolutions.tickets.infrastructure.security.identity;

import static uk.co.techsavvysolutions.tickets.TicketFixtures.ENGINEER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import uk.co.techsavvysolutions.tickets.domain.identity.NoCurrentUserException;
import java.util.concurrent.StructuredTaskScope;
import org.junit.jupiter.api.Test;

class AuthenticatedUserProviderAdapterTest {

    private final AuthenticatedUserProviderAdapter provider = new AuthenticatedUserProviderAdapter();

    @Test
    void failsClearlyWhenNobodyIsSignedIn() {
        assertThatThrownBy(provider::require).isInstanceOf(NoCurrentUserException.class);
    }

    @Test
    void returnsTheBoundUser() {
        var user = ScopedValue.where(AuthenticatedUserContext.CURRENT_USER, ENGINEER).call(provider::require);

        assertThat(user).isEqualTo(ENGINEER);
    }

    @Test
    void forkedVirtualThreadsInheritTheBinding() throws Exception {
        var seen = ScopedValue.where(AuthenticatedUserContext.CURRENT_USER, ENGINEER).call(() -> {
            try (var scope = StructuredTaskScope.open()) {
                var subtask = scope.fork(provider::require);
                scope.join();
                return subtask.get();
            }
        });

        assertThat(seen).isEqualTo(ENGINEER);
    }
}
