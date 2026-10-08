package pe.upc.peaceapp.identity.domain.model;

import org.junit.jupiter.api.Test;
import pe.upc.peaceapp.identity.domain.exception.DomainRuleViolationException;
import pe.upc.peaceapp.identity.domain.exception.ResourceNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final Duration LOCK = Duration.ofMinutes(15);

    private User citizen() {
        return User.registerCitizen("ciudadano@peaceapp.pe", "hash", "token-hash", NOW.plusSeconds(3600), NOW);
    }

    @Test
    void registeredCitizenStartsUnverifiedWithVerificationToken() {
        User user = citizen();

        assertThat(user.getRole()).isEqualTo(Role.CIUDADANO);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getVerificationTokenHash()).isEqualTo("token-hash");
        assertThat(user.isVerificationTokenExpired(NOW)).isFalse();
        assertThat(user.isVerificationTokenExpired(NOW.plusSeconds(3600))).isTrue();
    }

    @Test
    void verifyEmailClearsToken() {
        User user = citizen();

        user.verifyEmail();

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationTokenHash()).isNull();
    }

    @Test
    void locksOnFifthConsecutiveFailureAndUnlocksAfterDuration() {
        User user = citizen();

        for (int i = 1; i <= 4; i++) {
            assertThat(user.registerFailedLogin(5, LOCK, NOW)).isFalse();
            assertThat(user.isLockedAt(NOW)).isFalse();
        }
        assertThat(user.registerFailedLogin(5, LOCK, NOW)).isTrue();

        assertThat(user.isLockedAt(NOW)).isTrue();
        assertThat(user.isLockedAt(NOW.plus(LOCK).minusSeconds(1))).isTrue();
        assertThat(user.isLockedAt(NOW.plus(LOCK))).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        User user = citizen();
        user.registerFailedLogin(5, LOCK, NOW);
        user.registerFailedLogin(5, LOCK, NOW);

        user.registerSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
        for (int i = 1; i <= 4; i++) {
            user.registerFailedLogin(5, LOCK, NOW);
        }
        assertThat(user.isLockedAt(NOW)).isFalse();
    }

    @Test
    void institutionalAccountRequiresJurisdiction() {
        assertThatThrownBy(() -> User.createVerified("muni@peaceapp.pe", "hash", Role.INSTITUCIONAL, null, NOW))
                .isInstanceOf(DomainRuleViolationException.class);

        User institutional = User.createVerified("muni@peaceapp.pe", "hash", Role.INSTITUCIONAL, "Miraflores", NOW);
        assertThat(institutional.getJurisdiction()).isEqualTo("Miraflores");
        assertThat(institutional.isEmailVerified()).isTrue();
    }

    @Test
    void changingRoleAwayFromInstitutionalDropsJurisdiction() {
        User user = User.createVerified("muni@peaceapp.pe", "hash", Role.INSTITUCIONAL, "Miraflores", NOW);

        user.changeRole(Role.MODERADOR, "Miraflores");

        assertThat(user.getRole()).isEqualTo(Role.MODERADOR);
        assertThat(user.getJurisdiction()).isNull();
    }

    @Test
    void interestZonesRespectMaximumAndCanBeRemoved() {
        User user = citizen();
        InterestZone home = user.addInterestZone("Casa", -12.1, -77.0, 500, 2, NOW);
        user.addInterestZone("Trabajo", -12.0, -77.1, 800, 2, NOW);

        assertThatThrownBy(() -> user.addInterestZone("Gym", -12.2, -77.2, 300, 2, NOW))
                .isInstanceOf(DomainRuleViolationException.class);

        user.removeInterestZone(home.getId());
        assertThat(user.getInterestZones()).extracting(InterestZone::getLabel).containsExactly("Trabajo");
        assertThatThrownBy(() -> user.removeInterestZone(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
