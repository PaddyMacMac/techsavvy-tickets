package uk.co.techsavvysolutions.tickets.identity.domain;

public interface CurrentUserProvider {

    /** The person making this request, or {@link NoCurrentUserException} if nobody is signed in. */
    CurrentUser require();
}
