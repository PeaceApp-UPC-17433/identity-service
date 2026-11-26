package pe.upc.peaceapp.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.upc.peaceapp.identity.domain.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * RNF-11: tokens de sesion con expiracion. CA-04: el token transporta userId, rol y jurisdiccion
 * para que el futuro API Gateway pueda propagar los claims sin consultar identity-service en cada peticion.
 */
@Service
public class JwtService {
    private static final String MFA_PURPOSE = "mfa-challenge";
    private static final long MFA_CHALLENGE_MINUTES = 5;


    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${identity.jwt.secret}") String secret,
            @Value("${identity.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String issueToken(User user) {
        return issueToken(user, false);
    }

    public String issueToken(User user, boolean mfaVerified) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("jurisdiction", user.getJurisdiction())
                .claim("emailVerified", user.isEmailVerified())
                .claim("mfa", mfaVerified)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public String issueMfaChallenge(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("purpose", MFA_PURPOSE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(MFA_CHALLENGE_MINUTES, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public java.util.UUID parseMfaChallenge(String token) {
        Claims claims = parseClaims(token);
        if (!MFA_PURPOSE.equals(claims.get("purpose", String.class))) throw new io.jsonwebtoken.JwtException("Token sin proposito MFA");
        return java.util.UUID.fromString(claims.getSubject());
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
