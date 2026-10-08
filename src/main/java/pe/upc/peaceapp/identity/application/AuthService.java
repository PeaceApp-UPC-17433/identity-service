package pe.upc.peaceapp.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.application.error.*;
import pe.upc.peaceapp.identity.config.IdentityProperties;
import pe.upc.peaceapp.identity.domain.event.IdentityEvent;
import pe.upc.peaceapp.identity.domain.model.RefreshToken;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.RefreshTokenRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.security.JwtService;
import pe.upc.peaceapp.identity.security.OpaqueTokens;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * US-01 registro y verificacion, US-02 inicio de sesion.
 * RNF-11: bloqueo temporal tras 5 intentos fallidos consecutivos, mensaje de error generico,
 * access token corto y refresh token rotativo.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EventOutbox eventOutbox;
    private final IdentityProperties properties;
    private final Clock clock;
    /** Se compara contra este hash cuando el correo no existe, para no revelar cuentas por tiempo de respuesta. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EventOutbox eventOutbox,
                       IdentityProperties properties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventOutbox = eventOutbox;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record RegistrationResult(UUID userId, String email, String verificationToken) {
    }

    public record AuthTokens(String accessToken, long expiresInSeconds, String refreshToken) {
    }

    @Transactional
    public RegistrationResult register(String rawEmail, String rawPassword) {
        String email = EmailAddress.normalize(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        Instant now = clock.instant();
        String verificationToken = OpaqueTokens.generate();
        Instant tokenExpiresAt = now.plus(properties.verification().tokenTtl());
        User user = User.registerCitizen(email, passwordEncoder.encode(rawPassword),
                OpaqueTokens.hash(verificationToken), tokenExpiresAt, now);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultaneos con el mismo correo: la restriccion unica decide.
            throw new EmailAlreadyRegisteredException();
        }
        eventOutbox.append(new IdentityEvent.UserRegistered(user.getId(), email, verificationToken, tokenExpiresAt));
        return new RegistrationResult(user.getId(), email, verificationToken);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationTokenHash(OpaqueTokens.hash(token))
                .orElseThrow(InvalidVerificationTokenException::new);
        if (user.isVerificationTokenExpired(clock.instant())) {
            throw new InvalidVerificationTokenException();
        }
        user.verifyEmail();
        eventOutbox.append(new IdentityEvent.UserVerified(user.getId(), user.getEmail()));
    }

    /**
     * Genera un nuevo token de verificacion. Si el correo no existe o ya esta verificado no hace nada,
     * y el controlador responde igual en todos los casos para no revelar que cuentas existen.
     */
    @Transactional
    public Optional<String> resendVerification(String rawEmail) {
        return userRepository.findByEmail(EmailAddress.normalize(rawEmail))
                .filter(user -> !user.isEmailVerified())
                .map(user -> {
                    Instant expiresAt = clock.instant().plus(properties.verification().tokenTtl());
                    String token = OpaqueTokens.generate();
                    user.issueVerificationToken(OpaqueTokens.hash(token), expiresAt);
                    eventOutbox.append(new IdentityEvent.VerificationRequested(
                            user.getId(), user.getEmail(), token, expiresAt));
                    return token;
                });
    }

    /**
     * Las excepciones de credenciales no revierten la transaccion: el contador de intentos fallidos
     * y el bloqueo deben persistir aunque la peticion falle.
     */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, EmailNotVerifiedException.class})
    public AuthTokens login(String rawEmail, String rawPassword) {
        Instant now = clock.instant();
        Optional<User> found = userRepository.findByEmail(EmailAddress.normalize(rawEmail));
        if (found.isEmpty()) {
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }
        User user = found.get();

        if (user.isLockedAt(now)) {
            log.warn("Intento de inicio de sesion sobre cuenta bloqueada {}", user.getId());
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            var security = properties.security();
            if (user.registerFailedLogin(security.maxFailedLoginAttempts(), security.lockDuration(), now)) {
                log.warn("Cuenta {} bloqueada hasta {}", user.getId(), user.getLockedUntil());
                eventOutbox.append(new IdentityEvent.AccountLocked(user.getId(), user.getLockedUntil()));
            }
            throw new InvalidCredentialsException();
        }

        user.registerSuccessfulLogin();
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        return issueTokens(user, now);
    }

    /** Rota el refresh token. Reusar uno ya revocado indica robo: se revocan todas las sesiones del usuario. */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            log.warn("Reuso de refresh token revocado para el usuario {}; se revocan todas sus sesiones",
                    current.getUserId());
            refreshTokenRepository.revokeAllActiveForUser(current.getUserId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(current.getUserId()).orElseThrow(InvalidRefreshTokenException::new);
        current.revoke(now);
        if (user.isLockedAt(now) || !user.isEmailVerified()) {
            throw new InvalidRefreshTokenException();
        }
        return issueTokens(user, now);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawRefreshToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private AuthTokens issueTokens(User user, Instant now) {
        String refreshToken = OpaqueTokens.generate();
        refreshTokenRepository.save(new RefreshToken(user.getId(), OpaqueTokens.hash(refreshToken),
                now.plus(properties.jwt().refreshTokenTtl()), now));
        return new AuthTokens(jwtService.issueAccessToken(user), jwtService.accessTokenTtlSeconds(), refreshToken);
    }
}
