package uk.co.techsavvysolutions.tickets.identity.infrastructure.security.identity;

import uk.co.techsavvysolutions.tickets.identity.domain.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Binds the signed-in user for the rest of the request, so services never touch Spring Security. */
@RequiredArgsConstructor
public class AuthenticatedUserContextFilter extends OncePerRequestFilter {

    private final JwtCurrentUserMapper mapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            proceedAs(mapper.fromJwt(token.getToken()), request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void proceedAs(
            CurrentUser user, HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            ScopedValue.where(AuthenticatedUserContext.CURRENT_USER, user).call(() -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
