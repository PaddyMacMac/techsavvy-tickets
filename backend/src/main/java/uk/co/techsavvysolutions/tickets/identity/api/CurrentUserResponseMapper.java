package uk.co.techsavvysolutions.tickets.identity.api;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUser;
import java.util.Comparator;
import org.springframework.stereotype.Component;

/** Converts the authenticated principal into the API's user response. */
@Component
class CurrentUserResponseMapper {

    CurrentUserResponse toResponse(CurrentUser user) {
        return new CurrentUserResponse(
                user.subject(),
                user.name(),
                user.email(),
                user.roles().stream().sorted(Comparator.naturalOrder()).toList());
    }
}
