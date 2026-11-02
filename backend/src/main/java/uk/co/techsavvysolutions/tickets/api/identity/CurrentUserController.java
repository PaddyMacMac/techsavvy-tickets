package uk.co.techsavvysolutions.tickets.api.identity;

import uk.co.techsavvysolutions.tickets.domain.identity.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CurrentUserController {

    private final CurrentUserProvider currentUserProvider;
    private final CurrentUserResponseMapper responseMapper;

    @GetMapping("/api/me")
    CurrentUserResponse getCurrentUser() {
        return responseMapper.toResponse(currentUserProvider.require());
    }
}
