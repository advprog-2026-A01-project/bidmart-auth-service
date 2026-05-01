package id.ac.ui.cs.advprog.backend.auth.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

    private final RSAKey rsaKey;

    public JwksController(final RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new com.nimbusds.jose.jwk.JWKSet(rsaKey.toPublicJWK())
                .toJSONObject();
    }
}
