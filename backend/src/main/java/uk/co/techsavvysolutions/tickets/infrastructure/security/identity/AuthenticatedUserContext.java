package uk.co.techsavvysolutions.tickets.infrastructure.security.identity;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUser;

/**
 * Scoped values replace ThreadLocals for per-request data. The binding is immutable, ends automatically
 * when the request finishes, and is inherited by virtual threads forked in a StructuredTaskScope.
 */
public final class AuthenticatedUserContext {

    public static final ScopedValue<CurrentUser> CURRENT_USER = ScopedValue.newInstance();

    private AuthenticatedUserContext() {}
}
