package uk.co.techsavvysolutions.tickets.identity.domain;

import uk.co.techsavvysolutions.tickets.shared.DomainException;
public final class NoCurrentUserException extends DomainException {

    public NoCurrentUserException() {
        super("No signed-in user for this request.");
    }
}
