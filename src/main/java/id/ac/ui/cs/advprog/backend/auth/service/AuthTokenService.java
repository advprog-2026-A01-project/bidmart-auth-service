package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.jwt.JwtTokenService;
import id.ac.ui.cs.advprog.backend.auth.model.AuthError;
import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;
import id.ac.ui.cs.advprog.backend.rbac.repository.RbacRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

    private static final String DEFAULT_ROLE = "BUYER";

    public record ClientMeta(String userAgent, String ip) {}

    public record IssuedTokenPair(
            String accessToken,
            UUID refreshToken,
            Instant accessExpiresAt,
            Instant refreshExpiresAt
    ) {}

    private final SessionRepository sessionRepository;
    private final UserAuthRepository userAuthRepository;
    private final RbacRepository rbacRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthProperties props;
    private final Clock clock;

    public AuthTokenService(
            final SessionRepository sessionRepository,
            final UserAuthRepository userAuthRepository,
            final RbacRepository rbacRepository,
            final JwtTokenService jwtTokenService,
            final AuthProperties props,
            final Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.userAuthRepository = userAuthRepository;
        this.rbacRepository = rbacRepository;
        this.jwtTokenService = jwtTokenService;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokenPair issue(final long userId, final Instant now, final ClientMeta meta) {
        final Duration accessTtl = Duration.ofMinutes(props.getAccessTtlMinutes());
        final Duration refreshTtl = Duration.ofDays(props.getRefreshTtlDays());

        final SessionRepository.TokenPair sessionTokens = sessionRepository.create(
                userId,
                now,
                now.plus(accessTtl),
                now.plus(refreshTtl),
                meta.userAgent(),
                meta.ip()
        );

        return issueJwtForSession(sessionTokens);
    }

    @Transactional
    public IssuedTokenPair refresh(final String refreshToken, final ClientMeta meta) {
        final Instant now = Instant.now(clock);
        final Duration accessTtl = Duration.ofMinutes(props.getAccessTtlMinutes());
        final Duration refreshTtl = Duration.ofDays(props.getRefreshTtlDays());

        final SessionRepository.TokenPair sessionTokens = sessionRepository.rotateByRefreshToken(
                refreshToken,
                now,
                now.plus(accessTtl),
                now.plus(refreshTtl),
                meta.userAgent(),
                meta.ip()
        ).orElseThrow(() -> AuthException.of(AuthError.INVALID_REFRESH_TOKEN));

        return issueJwtForSession(sessionTokens);
    }

    @Transactional
    public void logout(final String accessTokenId) {
        sessionRepository.revokeByAccessToken(accessTokenId, Instant.now(clock));
    }

    private IssuedTokenPair issueJwtForSession(final SessionRepository.TokenPair sessionTokens) {
        final var user = userAuthRepository.findById(sessionTokens.userId())
                .orElseThrow(() -> AuthException.of(AuthError.INVALID_CREDENTIALS));

        final String role = user.role() == null ? DEFAULT_ROLE : user.role().name();
        final List<String> permissions = rbacRepository.listPermissionsForRole(role);

        final String jwtAccessToken = jwtTokenService.issueAccessToken(
                sessionTokens.accessToken(),
                user.id(),
                user.username(),
                role,
                permissions
        );

        return new IssuedTokenPair(
                jwtAccessToken,
                sessionTokens.refreshToken(),
                sessionTokens.accessExpiresAt(),
                sessionTokens.refreshExpiresAt()
        );
    }
}