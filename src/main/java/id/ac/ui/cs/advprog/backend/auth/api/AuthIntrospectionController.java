package id.ac.ui.cs.advprog.backend.auth.api;

import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.rbac.repository.RbacRepository;
import id.ac.ui.cs.advprog.backend.security.TokenExtractor;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class AuthIntrospectionController {

    private static final String DEFAULT_ROLE = "BUYER";

    private final SessionRepository sessionRepository;
    private final RbacRepository rbacRepository;
    private final Clock clock;

    public AuthIntrospectionController(
            final SessionRepository sessionRepository,
            final RbacRepository rbacRepository,
            final Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.rbacRepository = rbacRepository;
        this.clock = clock;
    }

    @PostMapping("/introspect")
    public ResponseEntity<IntrospectionResponse> introspect(
            @RequestBody(required = false) final IntrospectionRequest body,
            final HttpServletRequest request
    ) {
        final String token = resolveToken(body, request);
        if (token.isBlank()) {
            return ResponseEntity.ok(IntrospectionResponse.inactive());
        }

        final var session = sessionRepository.findActiveByAccessToken(token, Instant.now(clock));
        if (session.isEmpty()) {
            return ResponseEntity.ok(IntrospectionResponse.inactive());
        }

        final var activeSession = session.get();
        final String role = normalizeRole(activeSession.role());
        final List<String> permissions = rbacRepository.listPermissionsForRole(role);

        return ResponseEntity.ok(new IntrospectionResponse(
                true,
                activeSession.userId(),
                activeSession.username(),
                role,
                permissions
        ));
    }

    private static String resolveToken(final IntrospectionRequest body, final HttpServletRequest request) {
        if (body != null && body.token() != null && !body.token().isBlank()) {
            return body.token().trim();
        }
        return TokenExtractor.extract(request).orElse("");
    }

    private static String normalizeRole(final String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }
        return role.trim();
    }

    public record IntrospectionRequest(String token) {}

    public record IntrospectionResponse(
            boolean active,
            Long userId,
            String username,
            String role,
            List<String> permissions
    ) {
        public static IntrospectionResponse inactive() {
            return new IntrospectionResponse(false, null, null, null, List.of());
        }
    }
}
