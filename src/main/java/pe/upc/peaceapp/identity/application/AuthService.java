package pe.upc.peaceapp.identity.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.api.dto.LoginResponse;
import pe.upc.peaceapp.identity.api.error.AccountLockedException;
import pe.upc.peaceapp.identity.api.error.EmailAlreadyRegisteredException;
import pe.upc.peaceapp.identity.api.error.InvalidCredentialsException;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.security.JwtService;

/**
 * US-01 registro, US-02 inicio de sesion.
 * RNF-11: bloqueo tras 5 intentos fallidos consecutivos, mensaje de error generico.
 */
@Service
public class AuthService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final int maxFailedLoginAttempts;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        @Value("${identity.security.max-failed-login-attempts}") int maxFailedLoginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
    }

    @Transactional
    public User register(String email, String rawPassword, String firstName, String lastName, String phoneNumber) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword));
        user.setFirstName(blankToNull(firstName));
        user.setLastName(blankToNull(lastName));
        user.setPhoneNumber(blankToNull(phoneNumber));
        User saved = userRepository.save(user);
        log.info("Usuario registrado userId={}", saved.getId());
        return saved;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email)).orElseThrow(() -> {
            log.info("Login rechazado: usuario inexistente");
            return new InvalidCredentialsException();
        });

        if (user.isLocked()) {
            log.warn("Login rechazado: cuenta bloqueada userId={}", user.getId());
            throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxFailedLoginAttempts) {
                user.setLocked(true);
                log.warn("Cuenta bloqueada por intentos fallidos userId={}", user.getId());
            }
            userRepository.save(user);
            log.info("Login rechazado: credenciales invalidas userId={} intentos={}", user.getId(), user.getFailedLoginAttempts());
            throw new InvalidCredentialsException();
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        log.info("Login correcto userId={}", user.getId());
        return new LoginResponse(jwtService.issueToken(user));
    }
}
