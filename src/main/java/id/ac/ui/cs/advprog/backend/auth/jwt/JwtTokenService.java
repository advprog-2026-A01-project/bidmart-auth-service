package id.ac.ui.cs.advprog.backend.auth.jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final String keyId;
    private final long accessTokenTtlMinutes;

    public JwtTokenService(
            final JwtEncoder jwtEncoder,
            final Clock clock,
            @Value("${jwt.issuer}") final String issuer,
            @Value("${jwt.key-id}") final String keyId,
            @Value("${jwt.access-token-ttl-minutes}") final long accessTokenTtlMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.keyId = keyId;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public String issueAccessToken(
            final UUID sessionTokenId,
            final Long userId,
            final String username,
            final String role,
            final List<String> permissions
    ) {
        final Instant now = Instant.now(clock);
        final List<String> safePermissions = permissions == null ? List.of() : permissions;

        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES))
                .id(sessionTokenId.toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("permissions", safePermissions)
                .build();

        final JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(keyId)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}