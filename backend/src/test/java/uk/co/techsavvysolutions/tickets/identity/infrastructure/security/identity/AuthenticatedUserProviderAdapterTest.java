package uk.co.techsavvysolutions.tickets.identity.infrastructure.security.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUser;
import uk.co.techsavvysolutions.tickets.identity.domain.NoCurrentUserException;
import uk.co.techsavvysolutions.tickets.identity.domain.Role;
import java.util.concurrent.StructuredTaskScope;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthenticatedUserProviderAdapterTest {

    private static final CurrentUser ENGINEER =
            new CurrentUser("eng-1", "Demo Engineer", "engineer@tss.local", Set.of(Role.ENGINEER));
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
