package pe.upc.peaceapp.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.api.error.InvalidResetTokenException;
import pe.upc.peaceapp.identity.domain.model.PasswordResetToken;
import pe.upc.peaceapp.identity.domain.repository.PasswordResetTokenRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.messaging.EmailNotifications;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotifications notifications;
    private final String webBaseUrl;
    private final Clock clock;
    private final Duration validity;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder passwordEncoder,
                                EmailNotifications notifications, Clock clock,
                                @Value("${identity.web-base-url}") String webBaseUrl,
                                @Value("${identity.password-reset.validity-minutes:30}") long validityMinutes) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.webBaseUrl = webBaseUrl.replaceAll("/+$", "");
        this.clock = clock;
        this.validity = Duration.ofMinutes(validityMinutes);
    }

    @Transactional
    public void request(String email) {
        users.findByEmail(AuthService.normalizeEmail(email)).ifPresentOrElse(user -> {
            byte[] raw = new byte[32];
            RANDOM.nextBytes(raw);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            Instant now = clock.instant();
            Instant expiresAt = now.plus(validity);
            tokens.save(new PasswordResetToken(user.getId(), hash(token), expiresAt, now));
            notifications.request("PASSWORD_RESET", user.getEmail(), user.getLocale(),
                    Map.of("resetUrl", webBaseUrl + "/reset-password?token=" + token, "expiresMinutes", String.valueOf(validity.toMinutes())));
            log.info("Recuperacion de contrasena solicitada userId={}", user.getId());
        }, () -> log.info("Recuperacion de contrasena solicitada para un correo inexistente"));
    }

    @Transactional
    public void reset(String token, String newPassword) {
        Instant now = clock.instant();
        var stored = tokens.findByTokenHash(hash(token)).filter(t -> t.isUsable(now)).orElseThrow(InvalidResetTokenException::new);
        var user = users.findById(stored.getUserId()).orElseThrow(InvalidResetTokenException::new);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        stored.setUsedAt(now);
        users.save(user);
        tokens.save(stored);
        notifications.request("PASSWORD_CHANGED", user.getEmail(), user.getLocale(), Map.of());
        log.info("Contrasena restablecida userId={}", user.getId());
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
