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
    public User register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User user = new User(email, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.isLocked()) {
            throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxFailedLoginAttempts) {
                user.setLocked(true);
            }
            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        return new LoginResponse(jwtService.issueToken(user));
    }
}
