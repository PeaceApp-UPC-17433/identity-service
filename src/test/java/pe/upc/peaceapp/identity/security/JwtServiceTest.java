
package pe.upc.peaceapp.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.upc.peaceapp.identity.domain.model.User;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "peaceapp-test-secret-key-12345678901234567890";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60);
    }

    @Test
    void issueTokenIncludesUserClaims() {
        User user = new User("usuario@peaceapp.pe", "hash");
        user.setId(UUID.randomUUID());

        String token = jwtService.issueToken(user);
        Claims claims = jwtService.parseClaims(token);

        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class))
                .isEqualTo("usuario@peaceapp.pe");
        assertThat(claims.get("role", String.class))
                .isEqualTo(user.getRole().name());
        assertThat(claims.get("jurisdiction"))
                .isEqualTo(user.getJurisdiction());
    }

    @Test
    void issueTokenHasCorrectExpiration() {
        User user = new User("usuario@peaceapp.pe", "hash");
        user.setId(UUID.randomUUID());

        String token = jwtService.issueToken(user);
        Claims claims = jwtService.parseClaims(token);

        long durationSeconds =
                (claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime()) / 1000;

        assertThat(durationSeconds).isEqualTo(3600);
    }

    @Test
    void parseClaimsRejectsInvalidToken() {
        assertThatThrownBy(() ->
                jwtService.parseClaims("token-invalido"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseClaimsRejectsTokenSignedWithDifferentKey() {
        JwtService otherService = new JwtService(
                "another-peaceapp-test-secret-12345678901234567890",
                60
        );

        User user = new User("usuario@peaceapp.pe", "hash");
        user.setId(UUID.randomUUID());

        String token = otherService.issueToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }
}
