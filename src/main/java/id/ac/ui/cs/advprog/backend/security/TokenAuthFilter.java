package id.ac.ui.cs.advprog.backend.security;

import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


/*
Tanggung jawab: middleware stateless:

- validasi token di DB
- set Authentication + ROLE_*
- simpan token ke auth.setDetails(token) untuk kebutuhan logout

 */

@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Auth-Token";

    private final SessionRepository sessionRepository;
    private final JwtDecoder jwtDecoder;

    public TokenAuthFilter(final SessionRepository sessionRepository, final JwtDecoder jwtDecoder) {
        this.sessionRepository = sessionRepository;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            TokenExtractor.extract(request).ifPresent(this::authenticateJwt);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateJwt(final String rawToken) {
        try {
            final Jwt jwt = jwtDecoder.decode(rawToken);
            final UUID sessionTokenId = UUID.fromString(jwt.getId());

            sessionRepository.findActiveByAccessToken(sessionTokenId.toString(), Instant.now()).ifPresent(session -> {
                final String roleName = normalizeRole(session.role());
                final var principal = new AuthPrincipal(session.userId(), session.username(), roleName);

                final List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + roleName)
                );

                final var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                auth.setDetails(sessionTokenId.toString());
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        } catch (JwtException | IllegalArgumentException ignored) {
            // Invalid token: leave SecurityContext empty so Spring Security returns 401.
        }
    }

    private static String normalizeRole(final String role) {
        if (role == null || role.isBlank()) {
            return "BUYER";
        }
        return role.trim();
    }
}