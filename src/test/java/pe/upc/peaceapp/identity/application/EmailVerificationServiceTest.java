package pe.upc.peaceapp.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.upc.peaceapp.identity.api.error.VerificationExceptions.*;
import pe.upc.peaceapp.identity.domain.model.EmailVerification;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.EmailVerificationRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.messaging.EmailNotifications;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {
    private static final String SECRET = "test-otp-secret-test-otp-secret-0123456789";

    private final Map<UUID, EmailVerification> store = new HashMap<>();
    private final CapturingPublisher publisher = new CapturingPublisher();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"));
    private UserRepository users;
    private EmailVerificationService service;
    private User user;

    static class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void advance(Duration d) { now = now.plus(d); }
        public ZoneId getZone() { return ZoneId.of("UTC"); }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        EmailVerificationRepository repository = mock(EmailVerificationRepository.class);
        when(repository.findById(any(UUID.class))).thenAnswer(i -> Optional.ofNullable(store.get(i.<UUID>getArgument(0))));
        when(repository.save(any(EmailVerification.class))).thenAnswer(i -> {
            EmailVerification v = i.getArgument(0);
            store.put(v.getUserId(), v);
            return v;
        });
        org.mockito.Mockito.doAnswer(i -> { store.remove(i.<EmailVerification>getArgument(0).getUserId()); return null; })
                .when(repository).delete(any(EmailVerification.class));
        service = new EmailVerificationService(users, repository, new EmailNotifications(publisher), clock, SECRET, 10, 5, 60);
        user = new User("ana@peaceapp.pe", "hash");
        user.setId(UUID.randomUUID());
        user.setLocale("es");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private String issuedCode() {
        return publisher.events.get(publisher.events.size() - 1).params().get("code");
    }

    @Test
    void issuePublishesASixDigitCodeAndStoresOnlyItsHash() {
        service.issue(user);
        String code = issuedCode();
        assertThat(code).matches("\\d{6}");
        assertThat(publisher.events.get(0).template()).isEqualTo("EMAIL_VERIFICATION");
        assertThat(publisher.events.get(0).params().get("expiresMinutes")).isEqualTo("10");
        assertThat(store.get(user.getId()).getCodeHash()).hasSize(64).doesNotContain(code);
    }

    @Test
    void correctCodeVerifiesTheUserAndConsumesTheCode() {
        service.issue(user);
        var result = service.confirm(user.getId(), " " + issuedCode() + " ");
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(store).isEmpty();
    }

    @Test
    void wrongCodesAreCountedAndTheAccountIsThrottledEvenForTheRightCode() {
        service.issue(user);
        String right = issuedCode();
        for (int i = 0; i < 5; i++) assertThatThrownBy(() -> service.confirm(user.getId(), "000000")).isInstanceOf(InvalidCodeException.class);
        assertThatThrownBy(() -> service.confirm(user.getId(), right)).isInstanceOf(TooManyAttemptsException.class);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void expiredCodesAreRejected() {
        service.issue(user);
        String code = issuedCode();
        clock.advance(Duration.ofMinutes(11));
        assertThatThrownBy(() -> service.confirm(user.getId(), code)).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void codeIsBoundToTheUser() {
        service.issue(user);
        String code = issuedCode();
        var other = new User("otra@peaceapp.pe", "hash");
        other.setId(UUID.randomUUID());
        when(users.findById(other.getId())).thenReturn(Optional.of(other));
        store.put(other.getId(), store.get(user.getId()));
        assertThatThrownBy(() -> service.confirm(other.getId(), code)).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void resendRespectsTheCooldownAndInvalidatesTheOldCode() {
        service.issue(user);
        String first = issuedCode();
        assertThatThrownBy(() -> service.resend(user.getId())).isInstanceOfSatisfying(ResendTooSoonException.class,
                e -> assertThat(e.retryAfterSeconds()).isBetween(1L, 60L));
        clock.advance(Duration.ofSeconds(61));
        service.resend(user.getId());
        assertThat(publisher.events).hasSize(2);
        String second = issuedCode();
        if (!first.equals(second)) assertThatThrownBy(() -> service.confirm(user.getId(), first)).isInstanceOf(InvalidCodeException.class);
        assertThat(service.confirm(user.getId(), second).isEmailVerified()).isTrue();
    }

    @Test
    void verifiedUsersCannotAskForAnotherCode() {
        user.setEmailVerified(true);
        assertThatThrownBy(() -> service.resend(user.getId())).isInstanceOf(AlreadyVerifiedException.class);
    }

    @Test
    void shortSecretsAreRejectedAtStartup() {
        assertThatThrownBy(() -> new EmailVerificationService(users, null, null, clock, "corta", 10, 5, 60))
                .isInstanceOf(IllegalStateException.class);
    }
}
