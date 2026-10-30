package pe.upc.peaceapp.identity.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.upc.peaceapp.identity.api.error.AccountLockedException;
import pe.upc.peaceapp.identity.api.error.EmailAlreadyRegisteredException;
import pe.upc.peaceapp.identity.api.error.InvalidCredentialsException;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.security.JwtService;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private AuthService newAuthService() {
        return new AuthService(userRepository, passwordEncoder, jwtService, MAX_FAILED_ATTEMPTS);
    }

    @Test
    void registerCreatesUserWhenEmailIsNew() {
        authService = newAuthService();
        when(userRepository.existsByEmail("nueva@peaceapp.pe")).thenReturn(false);
        when(passwordEncoder.encode("clave12345")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = authService.register("nueva@peaceapp.pe", "clave12345");

        assertThat(created.getEmail()).isEqualTo("nueva@peaceapp.pe");
        assertThat(created.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void registerFailsWhenEmailAlreadyRegistered() {
        authService = newAuthService();
        when(userRepository.existsByEmail("ya@peaceapp.pe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("ya@peaceapp.pe", "clave12345"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void loginLocksAccountAfterMaxFailedAttempts() {
        authService = newAuthService();
        User user = new User("ciudadano@peaceapp.pe", "hash");
        for (int i = 0; i < MAX_FAILED_ATTEMPTS - 1; i++) {
            user.setFailedLoginAttempts(i);
        }
        user.setFailedLoginAttempts(MAX_FAILED_ATTEMPTS - 1);

        when(userRepository.findByEmail("ciudadano@peaceapp.pe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ciudadano@peaceapp.pe", "incorrecta"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void loginFailsWhenAccountIsAlreadyLocked() {
        authService = newAuthService();
        User user = new User("bloqueado@peaceapp.pe", "hash");
        user.setLocked(true);
        when(userRepository.findByEmail("bloqueado@peaceapp.pe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("bloqueado@peaceapp.pe", "cualquier"))
                .isInstanceOf(AccountLockedException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }
    @Test
void registerNormalizesEmailToLowercase() {
    authService = newAuthService();

   when(userRepository.existsByEmail(anyString()))
        .thenReturn(false);

    when(passwordEncoder.encode("clave12345"))
            .thenReturn("hash");

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    User created = authService.register(
            "Usuario@PeaceApp.pe", "clave12345"
    );

    assertThat(created.getEmail())
            .isEqualTo("usuario@peaceapp.pe");

    verify(userRepository)
            .existsByEmail("usuario@peaceapp.pe");
}
}
