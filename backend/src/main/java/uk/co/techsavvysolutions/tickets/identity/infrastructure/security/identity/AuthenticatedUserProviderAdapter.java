package uk.co.techsavvysolutions.tickets.identity.infrastructure.security.identity;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUser;
import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.identity.domain.NoCurrentUserException;
import org.springframework.stereotype.Component;

@Component
class AuthenticatedUserProviderAdapter implements CurrentUserProvider {

    @Override
    public CurrentUser require() {
        return AuthenticatedUserContext.CURRENT_USER.orElseThrow(NoCurrentUserException::new);
    }
}
