package pe.upc.peaceapp.identity.application.mfa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.api.error.CurrentPasswordMismatchException;
import pe.upc.peaceapp.identity.api.error.MfaExceptions.*;
import pe.upc.peaceapp.identity.domain.model.MfaRecoveryCode;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.MfaRecoveryCodeRepository;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MfaService {
    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RECOVERY_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int RECOVERY_CODES = 8;
    private static final Pattern RECOVERY_FORMAT = Pattern.compile("^[a-z2-9]{10}$");

    public record Setup(String secret, String otpauthUri) {
    }

    private final UserRepository users;
    private final MfaRecoveryCodeRepository recoveryCodes;
    private final TotpService totp;
    private final SecretCipher cipher;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotifications notifications;
    private final Clock clock;
    private final byte[] hashKey;
    private final int maxFailedAttempts;
    private final Duration lockDuration;
    private final boolean requiredForStaff;

    public MfaService(UserRepository users, MfaRecoveryCodeRepository recoveryCodes, TotpService totp, SecretCipher cipher,
                      PasswordEncoder passwordEncoder, EmailNotifications notifications, Clock clock,
                      @Value("${identity.verification.otp-secret}") String secret,
                      @Value("${identity.mfa.max-failed-attempts:5}") int maxFailedAttempts,
                      @Value("${identity.mfa.lock-minutes:5}") long lockMinutes,
                      @Value("${identity.mfa.required-for-staff:true}") boolean requiredForStaff) {
        this.users = users;
        this.recoveryCodes = recoveryCodes;
        this.totp = totp;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.clock = clock;
        this.hashKey = secret.getBytes(StandardCharsets.UTF_8);
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.requiredForStaff = requiredForStaff;
    }

    @Transactional
    public Setup setup(UUID userId) {
        User user = lock(userId);
        if (user.isMfaEnabled()) throw new MfaStateException("MFA_ALREADY_ENABLED", "La verificacion en dos pasos ya esta activa");
        String secret = totp.newSecret();
        user.setMfaPendingSecret(cipher.encrypt(secret));
        users.save(user);
        log.info("Configuracion de MFA iniciada userId={}", userId);
        return new Setup(secret, totp.otpauthUri(user.getEmail(), secret));
    }

    @Transactional(noRollbackFor = {InvalidMfaCodeException.class, MfaLockedException.class})
    public List<String> enable(UUID userId, String code) {
        User user = lock(userId);
        if (user.isMfaEnabled()) throw new MfaStateException("MFA_ALREADY_ENABLED", "La verificacion en dos pasos ya esta activa");
        if (user.getMfaPendingSecret() == null) throw new MfaStateException("MFA_NOT_IN_SETUP", "Primero inicia la configuracion");
        throwIfLocked(user);
        String secret = cipher.decrypt(user.getMfaPendingSecret());
        var step = totp.verify(secret, code, clock.instant(), 0);
        if (step.isEmpty()) throw failed(user);
        user.setMfaSecret(cipher.encrypt(secret));
        user.setMfaPendingSecret(null);
        user.setMfaEnabled(true);
        user.setMfaLastStep(step.getAsLong());
        resetFailures(user);
        users.save(user);
        List<String> codes = newRecoveryCodes(user);
        notifications.request("MFA_ENABLED", user.getEmail(), user.getLocale(), Map.of());
        log.info("MFA activado userId={}", userId);
        return codes;
    }

    @Transactional(noRollbackFor = {InvalidMfaCodeException.class, MfaLockedException.class})
    public User verifyLogin(UUID userId, String code) {
        User user = lock(userId);
        if (!user.isMfaEnabled()) throw new MfaStateException("MFA_NOT_ENABLED", "La verificacion en dos pasos no esta activa");
        secondFactor(user, code);
        log.info("Segundo factor correcto userId={}", userId);
        return user;
    }

    @Transactional(noRollbackFor = {InvalidMfaCodeException.class, MfaLockedException.class, CurrentPasswordMismatchException.class})
    public void disable(UUID userId, String password, String code) {
        User user = lock(userId);
        requireEnabled(user);
        if (user.isStaff() && requiredForStaff)
            throw new MfaStateException("MFA_REQUIRED_FOR_STAFF", "Los moderadores y administradores no pueden desactivar la verificacion en dos pasos");
        confirmIdentity(user, password, code);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaPendingSecret(null);
        user.setMfaLastStep(0);
        users.save(user);
        recoveryCodes.deleteByUserId(userId);
        notifications.request("MFA_DISABLED", user.getEmail(), user.getLocale(), Map.of());
        log.warn("MFA desactivado userId={}", userId);
    }

    @Transactional(noRollbackFor = {InvalidMfaCodeException.class, MfaLockedException.class, CurrentPasswordMismatchException.class})
    public List<String> regenerateRecoveryCodes(UUID userId, String password, String code) {
        User user = lock(userId);
        requireEnabled(user);
        confirmIdentity(user, password, code);
        List<String> codes = newRecoveryCodes(user);
        log.info("Codigos de recuperacion regenerados userId={}", userId);
        return codes;
    }

    public long remainingRecoveryCodes(UUID userId) {
        return recoveryCodes.countByUserIdAndUsedAtIsNull(userId);
    }

    private void confirmIdentity(User user, String password, String code) {
        if (!passwordEncoder.matches(password == null ? "" : password, user.getPasswordHash())) throw new CurrentPasswordMismatchException();
        secondFactor(user, code);
    }

    private void secondFactor(User user, String rawCode) {
        throwIfLocked(user);
        String code = rawCode == null ? "" : rawCode.trim();
        Instant now = clock.instant();
        var step = totp.verify(cipher.decrypt(user.getMfaSecret()), code, now, user.getMfaLastStep());
        if (step.isPresent()) {
            user.setMfaLastStep(step.getAsLong());
            resetFailures(user);
            users.save(user);
            return;
        }
        String normalized = code.toLowerCase().replace("-", "").replace(" ", "");
        if (RECOVERY_FORMAT.matcher(normalized).matches()) {
            String hash = recoveryHash(user.getId(), normalized);
            for (MfaRecoveryCode candidate : recoveryCodes.findByUserIdAndUsedAtIsNull(user.getId())) {
                if (MessageDigest.isEqual(candidate.getCodeHash().getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8))) {
                    candidate.setUsedAt(now);
                    recoveryCodes.save(candidate);
                    resetFailures(user);
                    users.save(user);
                    log.warn("Codigo de recuperacion usado userId={} restantes={}", user.getId(),
                            recoveryCodes.countByUserIdAndUsedAtIsNull(user.getId()));
                    return;
                }
            }
        }
        throw failed(user);
    }

    private InvalidMfaCodeException failed(User user) {
        user.setMfaFailedAttempts(user.getMfaFailedAttempts() + 1);
        if (user.getMfaFailedAttempts() >= maxFailedAttempts) {
            user.setMfaLockedUntil(clock.instant().plus(lockDuration));
            user.setMfaFailedAttempts(0);
            log.warn("MFA bloqueado temporalmente por intentos fallidos userId={}", user.getId());
        }
        users.save(user);
        log.info("Codigo MFA incorrecto userId={}", user.getId());
        return new InvalidMfaCodeException();
    }

    private void throwIfLocked(User user) {
        Instant until = user.getMfaLockedUntil();
        if (until != null && until.isAfter(clock.instant()))
            throw new MfaLockedException(Duration.between(clock.instant(), until).toSeconds() + 1);
    }

    private void resetFailures(User user) {
        user.setMfaFailedAttempts(0);
        user.setMfaLockedUntil(null);
    }

    private List<String> newRecoveryCodes(User user) {
        recoveryCodes.deleteByUserId(user.getId());
        recoveryCodes.flush();
        List<String> plain = new ArrayList<>();
        Instant now = clock.instant();
        for (int i = 0; i < RECOVERY_CODES; i++) {
            StringBuilder code = new StringBuilder();
            for (int c = 0; c < 10; c++) code.append(RECOVERY_ALPHABET.charAt(RANDOM.nextInt(RECOVERY_ALPHABET.length())));
            String value = code.toString();
            recoveryCodes.save(new MfaRecoveryCode(user.getId(), recoveryHash(user.getId(), value), now));
            plain.add(value.substring(0, 5) + "-" + value.substring(5));
        }
        return plain;
    }

    private String recoveryHash(UUID userId, String normalizedCode) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("recovery:" + userId + ":" + normalizedCode).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private void requireEnabled(User user) {
        if (!user.isMfaEnabled()) throw new MfaStateException("MFA_NOT_ENABLED", "La verificacion en dos pasos no esta activa");
    }

    private User lock(UUID userId) {
        return users.lockById(userId).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }
}
