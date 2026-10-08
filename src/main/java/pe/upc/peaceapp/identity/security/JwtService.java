package pe.upc.peaceapp.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import pe.upc.peaceapp.identity.config.IdentityProperties;
import pe.upc.peaceapp.identity.domain.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * RNF-11: tokens de acceso con expiracion. CA-04: el token transporta userId, rol y jurisdiccion
 * para que el API Gateway propague los claims sin consultar identity-service en cada peticion.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final IdentityProperties.Jwt properties;
    private final Clock clock;

    public JwtService(IdentityProperties properties, Clock clock) {
        this.properties = properties.jwt();
        this.signingKey = Keys.hmacShaKeyFor(this.properties.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String issueAccessToken(User user) {
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())));
        if (user.getJurisdiction() != null) {
            builder.claim("jurisdiction", user.getJurisdiction());
        }
        return builder.signWith(signingKey).compact();
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
