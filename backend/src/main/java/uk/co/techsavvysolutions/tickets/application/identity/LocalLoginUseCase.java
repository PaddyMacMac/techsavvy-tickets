package uk.co.techsavvysolutions.tickets.application.identity;

import java.util.Optional;

/** Exchanges valid local demo-account credentials for an access token. */
public interface LocalLoginUseCase {

    Optional<IssuedAccessToken> authenticate(String email, String password);
}
