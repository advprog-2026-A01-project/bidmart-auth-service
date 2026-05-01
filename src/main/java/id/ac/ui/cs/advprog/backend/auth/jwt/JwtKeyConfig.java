package id.ac.ui.cs.advprog.backend.auth.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.Assert;

@Configuration
public class JwtKeyConfig {

    @Bean
    public RSAKey rsaKey(
            @Value("${jwt.key-id}") final String keyId,
            @Value("${jwt.private-key-base64}") final String privateKeyBase64,
            @Value("${jwt.public-key-base64}") final String publicKeyBase64
    ) throws Exception {
        if (privateKeyBase64 == null || privateKeyBase64.isBlank()) {
            if (publicKeyBase64 != null && !publicKeyBase64.isBlank()) {
                throw new IllegalStateException("JWT_PUBLIC_KEY_BASE64 is set but JWT_PRIVATE_KEY_BASE64 is empty");
            }
            return generateDevelopmentKey(keyId);
        }

        Assert.hasText(publicKeyBase64, "JWT_PUBLIC_KEY_BASE64 must be configured");

        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        final RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(decodePrivateKey(privateKeyBase64))
        );

        final RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                new X509EncodedKeySpec(decodePublicKey(publicKeyBase64))
        );

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyId)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(final RSAKey rsaKey) {
        final JWKSource<SecurityContext> jwkSource = (selector, context) ->
                selector.select(new com.nimbusds.jose.jwk.JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(final RSAKey rsaKey) throws Exception {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }

    private static RSAKey generateDevelopmentKey(final String keyId) throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        final var keyPair = generator.generateKeyPair();
        final var publicKey = (RSAPublicKey) keyPair.getPublic();
        final var privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyId)
                .build();
    }

    private static byte[] decodePrivateKey(final String privateKeyBase64) {
        final String pem = decodeBase64ToText(privateKeyBase64);
        final String body = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static byte[] decodePublicKey(final String publicKeyBase64) {
        final String pem = decodeBase64ToText(publicKeyBase64);
        final String body = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static String decodeBase64ToText(final String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}