package uk.co.techsavvysolutions.tickets.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.co.techsavvysolutions.tickets.identity.application.LocalLoginUseCase;

/** HTTP adapter for the local demo-account sign-in flow. */
@RestController
@Profile("local")
@RequiredArgsConstructor
class LocalLoginController {

    private final LocalLoginUseCase localLogin;

    record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    record LoginResponse(String accessToken, long expiresInSeconds) {}

    @PostMapping("/api/auth/dev-login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var token = localLogin.authenticate(request.email(), request.password())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Incorrect email or password."));
        return new LoginResponse(token.accessToken(), token.expiresInSeconds());
    }
}
