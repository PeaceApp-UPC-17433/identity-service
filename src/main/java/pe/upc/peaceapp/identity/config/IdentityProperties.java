package pe.upc.peaceapp.identity.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Configuracion del bounded context; se valida al arrancar para fallar rapido ante valores inseguros. */
@Validated
@ConfigurationProperties(prefix = "identity")
public record IdentityProperties(
        @Valid Jwt jwt,
        @Valid Security security,
        @Valid Verification verification,
        @Valid Profile profile,
        BootstrapAdmin bootstrapAdmin
) {

    public record Jwt(
            @NotBlank(message = "JWT_SECRET es obligatorio")
            @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres") String secret,
            @NotBlank String issuer,
            @Min(1) long accessTokenMinutes,
            @Min(1) long refreshTokenDays
    ) {
        public Duration accessTokenTtl() {
            return Duration.ofMinutes(accessTokenMinutes);
        }

        public Duration refreshTokenTtl() {
            return Duration.ofDays(refreshTokenDays);
        }
    }

    public record Security(@Min(1) int maxFailedLoginAttempts, @Min(1) long lockDurationMinutes) {
        public Duration lockDuration() {
            return Duration.ofMinutes(lockDurationMinutes);
        }
    }

    public record Verification(@Min(1) long tokenHours, boolean exposeTokenInResponse) {
        public Duration tokenTtl() {
            return Duration.ofHours(tokenHours);
        }
    }

    public record Profile(@Min(1) int maxInterestZones) {
    }

    public record BootstrapAdmin(String email, String password) {
        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }
}
