package pe.upc.peaceapp.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.api.error.VerificationExceptions.*;
import pe.upc.peaceapp.identity.domain.model.EmailVerification;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.EmailVerificationRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.messaging.EmailNotifications;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final EmailVerificationRepository verifications;
    private final EmailNotifications notifications;
    private final Clock clock;
    private final byte[] secret;
    private final Duration ttl;
    private final int maxAttempts;
    private final Duration resendCooldown;

    public EmailVerificationService(UserRepository users, EmailVerificationRepository verifications, EmailNotifications notifications,
                                    Clock clock, @Value("${identity.verification.otp-secret}") String secret,
                                    @Value("${identity.verification.code-ttl-minutes}") long ttlMinutes,
                                    @Value("${identity.verification.max-attempts}") int maxAttempts,
                                    @Value("${identity.verification.resend-cooldown-seconds}") long cooldownSeconds) {
        if (secret.length() < 32) throw new IllegalStateException("OTP_SECRET debe tener al menos 32 caracteres");
        this.users = users;
        this.verifications = verifications;
        this.notifications = notifications;
        this.clock = clock;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.maxAttempts = maxAttempts;
        this.resendCooldown = Duration.ofSeconds(cooldownSeconds);
    }

    @Transactional
    public void issue(User user) {
        Instant now = clock.instant();
        String code = newCode();
        String hash = hash(user.getId(), code);
        var verification = verifications.findById(user.getId()).orElse(null);
        if (verification == null) verifications.save(new EmailVerification(user.getId(), hash, now.plus(ttl), now));
        else verification.renew(hash, now.plus(ttl), now);
        notifications.request("EMAIL_VERIFICATION", user.getEmail(), user.getLocale(),
                Map.of("code", code, "expiresMinutes", String.valueOf(ttl.toMinutes())));
        log.info("Codigo de verificacion emitido userId={}", user.getId());
    }

    @Transactional
    public void resend(UUID userId) {
        User user = find(userId);
        if (user.isEmailVerified()) throw new AlreadyVerifiedException();
        verifications.findById(userId).ifPresent(v -> {
            long wait = resendCooldown.minus(Duration.between(v.getLastSentAt(), clock.instant())).toSeconds();
            if (wait > 0) throw new ResendTooSoonException(wait);
        });
        issue(user);
    }

    @Transactional(noRollbackFor = {InvalidCodeException.class, TooManyAttemptsException.class})
    public User confirm(UUID userId, String code) {
        User user = find(userId);
        if (user.isEmailVerified()) return user;
        var verification = verifications.findById(userId).orElseThrow(InvalidCodeException::new);
        if (verification.getExpiresAt().isBefore(clock.instant())) throw new InvalidCodeException();
        if (verification.getAttempts() >= maxAttempts) throw new TooManyAttemptsException();
        verification.setAttempts(verification.getAttempts() + 1);
        if (!matches(userId, code, verification.getCodeHash())) {
            log.info("Codigo de verificacion incorrecto userId={} intentos={}", userId, verification.getAttempts());
            throw new InvalidCodeException();
        }
        user.setEmailVerified(true);
        users.save(user);
        verifications.delete(verification);
        log.info("Correo verificado userId={}", userId);
        return user;
    }

    private boolean matches(UUID userId, String code, String expectedHash) {
        return MessageDigest.isEqual(hash(userId, code == null ? "" : code.trim()).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    private String hash(UUID userId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String newCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }
}
