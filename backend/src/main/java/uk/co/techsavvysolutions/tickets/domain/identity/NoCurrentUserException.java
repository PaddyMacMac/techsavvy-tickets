package uk.co.techsavvysolutions.tickets.domain.identity;

import uk.co.techsavvysolutions.tickets.domain.DomainException;
public final class NoCurrentUserException extends DomainException {

    public NoCurrentUserException() {
        super("No signed-in user for this request.");
    }
}
