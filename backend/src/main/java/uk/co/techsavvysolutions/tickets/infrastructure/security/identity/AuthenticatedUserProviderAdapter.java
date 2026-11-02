package uk.co.techsavvysolutions.tickets.infrastructure.security.identity;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUser;
import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import uk.co.techsavvysolutions.tickets.domain.identity.NoCurrentUserException;
import org.springframework.stereotype.Component;

@Component
class AuthenticatedUserProviderAdapter implements CurrentUserProvider {

    @Override
    public CurrentUser require() {
        return AuthenticatedUserContext.CURRENT_USER.orElseThrow(NoCurrentUserException::new);
    }
}
