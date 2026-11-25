package pe.upc.peaceapp.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.upc.peaceapp.identity.api.error.CurrentPasswordMismatchException;
import pe.upc.peaceapp.identity.api.error.MfaExceptions.*;
import pe.upc.peaceapp.identity.application.mfa.MfaService;
import pe.upc.peaceapp.identity.application.mfa.SecretCipher;
import pe.upc.peaceapp.identity.application.mfa.TotpService;
import pe.upc.peaceapp.identity.domain.model.MfaRecoveryCode;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.MfaRecoveryCodeRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.messaging.EmailNotifications;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MfaServiceTest {
    private static final String SECRET = "test-otp-secret-test-otp-secret-0123456789";
    private static final String PASSWORD = "Clave-segura-77";

    static class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void advance(Duration d) { now = now.plus(d); }
        public ZoneId getZone() { return ZoneId.of("UTC"); }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    private final CapturingPublisher publisher = new CapturingPublisher();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-25T12:00:10Z"));
    private final List<MfaRecoveryCode> codes = new ArrayList<>();
    private final TotpService totp = new TotpService("PeaceApp");
    private final SecretCipher cipher = new SecretCipher(SECRET);
    private MfaService service;
    private User user;

    @BeforeEach
    void setUp() {
        UserRepository users = mock(UserRepository.class);
        MfaRecoveryCodeRepository recovery = mock(MfaRecoveryCodeRepository.class);
        user = new User("ana@peaceapp.pe", new BCryptPasswordEncoder(4).encode(PASSWORD));
        user.setId(UUID.randomUUID());
        user.setLocale("es");
        when(users.lockById(user.getId())).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(recovery.save(any(MfaRecoveryCode.class))).thenAnswer(i -> {
            codes.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(recovery.findByUserIdAndUsedAtIsNull(any(UUID.class))).thenAnswer(i -> codes.stream().filter(c -> c.getUsedAt() == null).toList());
        when(recovery.countByUserIdAndUsedAtIsNull(any(UUID.class))).thenAnswer(i -> codes.stream().filter(c -> c.getUsedAt() == null).count());
        doAnswer(i -> { codes.clear(); return null; }).when(recovery).deleteByUserId(any(UUID.class));
        service = new MfaService(users, recovery, totp, cipher, new BCryptPasswordEncoder(4), new EmailNotifications(publisher), clock,
                SECRET, 3, 5, true);
    }

    private String currentCode() {
        return totp.codeAt(cipher.decrypt(user.getMfaSecret()), clock.instant());
    }

    private List<String> enable() {
        var setup = service.setup(user.getId());
        return service.enable(user.getId(), totp.codeAt(setup.secret(), clock.instant()));
    }

    @Test
    void setupDoesNotEnableMfaUntilTheFirstCodeIsConfirmed() {
        var setup = service.setup(user.getId());
        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(setup.otpauthUri()).startsWith("otpauth://totp/PeaceApp:ana%40peaceapp.pe?secret=" + setup.secret());
        assertThat(user.getMfaPendingSecret()).doesNotContain(setup.secret());
    }

    @Test
    void enablingWithTheRightCodeActivatesMfaAndReturnsRecoveryCodes() {
        var recovery = enable();
        assertThat(user.isMfaEnabled()).isTrue();
        assertThat(user.getMfaPendingSecret()).isNull();
        assertThat(recovery).hasSize(8).allMatch(c -> c.matches("[a-z2-9]{5}-[a-z2-9]{5}"));
        assertThat(codes).hasSize(8).noneMatch(c -> recovery.contains(c.getCodeHash()));
        assertThat(publisher.events).anyMatch(e -> "MFA_ENABLED".equals(e.template()));
    }

    @Test
    void enablingWithAWrongCodeKeepsMfaOff() {
        service.setup(user.getId());
        assertThatThrownBy(() -> service.enable(user.getId(), "000000")).isInstanceOf(InvalidMfaCodeException.class);
        assertThat(user.isMfaEnabled()).isFalse();
    }

    @Test
    void enablingWithoutSetupIsRejected() {
        assertThatThrownBy(() -> service.enable(user.getId(), "123456")).isInstanceOf(MfaStateException.class);
    }

    @Test
    void secretIsStoredEncrypted() {
        enable();
        assertThat(user.getMfaSecret()).startsWith("v1:");
        assertThat(cipher.decrypt(user.getMfaSecret())).matches("[A-Z2-7]{32}");
    }

    @Test
    void aCodeCannotBeUsedTwice() {
        enable();
        clock.advance(Duration.ofSeconds(31));
        String code = currentCode();
        service.verifyLogin(user.getId(), code);
        assertThatThrownBy(() -> service.verifyLogin(user.getId(), code)).isInstanceOf(InvalidMfaCodeException.class);
    }

    @Test
    void theCodeUsedToEnableCannotLogInRightAfter() {
        var setup = service.setup(user.getId());
        String code = totp.codeAt(setup.secret(), clock.instant());
        service.enable(user.getId(), code);
        assertThatThrownBy(() -> service.verifyLogin(user.getId(), code)).isInstanceOf(InvalidMfaCodeException.class);
    }

    @Test
    void codesFromOneStepAwayAreAcceptedButTwoStepsAreNot() {
        enable();
        String secret = cipher.decrypt(user.getMfaSecret());
        clock.advance(Duration.ofSeconds(61));
        service.verifyLogin(user.getId(), totp.codeAt(secret, clock.instant().minusSeconds(30)));
        clock.advance(Duration.ofSeconds(120));
        assertThatThrownBy(() -> service.verifyLogin(user.getId(), totp.codeAt(secret, clock.instant().minusSeconds(75))))
                .isInstanceOf(InvalidMfaCodeException.class);
    }

    @Test
    void nonNumericOrShortCodesAreRejectedWithoutError() {
        enable();
        for (String bad : new String[]{"", "abc123", "12345", "1234567", " "}) {
            assertThatThrownBy(() -> service.verifyLogin(user.getId(), bad)).isInstanceOf(InvalidMfaCodeException.class);
            clock.advance(Duration.ofMinutes(6));
        }
    }

    @Test
    void repeatedFailuresLockTemporarilyEvenForTheRightCode() {
        enable();
        clock.advance(Duration.ofSeconds(31));
        for (int i = 0; i < 3; i++)
            assertThatThrownBy(() -> service.verifyLogin(user.getId(), "000000")).isInstanceOf(InvalidMfaCodeException.class);
        assertThatThrownBy(() -> service.verifyLogin(user.getId(), currentCode()))
                .isInstanceOfSatisfying(MfaLockedException.class, e -> assertThat(e.retryAfterSeconds()).isBetween(1L, 301L));
        clock.advance(Duration.ofMinutes(6));
        assertThat(service.verifyLogin(user.getId(), currentCode())).isSameAs(user);
    }

    @Test
    void recoveryCodeWorksOnceAndIsCaseAndDashInsensitive() {
        var recovery = enable();
        String first = recovery.get(0);
        service.verifyLogin(user.getId(), first.toUpperCase());
        assertThatThrownBy(() -> service.verifyLogin(user.getId(), first)).isInstanceOf(InvalidMfaCodeException.class);
        assertThat(service.remainingRecoveryCodes(user.getId())).isEqualTo(7);
        service.verifyLogin(user.getId(), recovery.get(1).replace("-", ""));
    }

    @Test
    void regeneratingRecoveryCodesInvalidatesTheOldOnes() {
        var old = enable();
        clock.advance(Duration.ofSeconds(31));
        var fresh = service.regenerateRecoveryCodes(user.getId(), PASSWORD, currentCode());
        assertThat(fresh).hasSize(8).doesNotContainAnyElementsOf(old);
        assertThatThrownBy(() -> service.verifyLogin(user.getId(), old.get(0))).isInstanceOf(InvalidMfaCodeException.class);
        service.verifyLogin(user.getId(), fresh.get(0));
    }

    @Test
    void citizenCanDisableWithPasswordAndCode() {
        enable();
        clock.advance(Duration.ofSeconds(31));
        service.disable(user.getId(), PASSWORD, currentCode());
        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(user.getMfaSecret()).isNull();
        assertThat(codes).isEmpty();
        assertThat(publisher.events).anyMatch(e -> "MFA_DISABLED".equals(e.template()));
    }

    @Test
    void disablingNeedsTheCorrectPassword() {
        enable();
        clock.advance(Duration.ofSeconds(31));
        assertThatThrownBy(() -> service.disable(user.getId(), "otra-clave", currentCode())).isInstanceOf(CurrentPasswordMismatchException.class);
        assertThat(user.isMfaEnabled()).isTrue();
    }

    @Test
    void staffCannotDisableMfa() {
        user.setRole(Role.MODERADOR);
        enable();
        clock.advance(Duration.ofSeconds(31));
        assertThatThrownBy(() -> service.disable(user.getId(), PASSWORD, currentCode()))
                .isInstanceOfSatisfying(MfaStateException.class, e -> assertThat(e.code()).isEqualTo("MFA_REQUIRED_FOR_STAFF"));
        assertThat(user.isMfaEnabled()).isTrue();
    }

    @Test
    void settingUpTwiceWhenAlreadyEnabledIsRejected() {
        enable();
        assertThatThrownBy(() -> service.setup(user.getId())).isInstanceOf(MfaStateException.class);
    }
}
