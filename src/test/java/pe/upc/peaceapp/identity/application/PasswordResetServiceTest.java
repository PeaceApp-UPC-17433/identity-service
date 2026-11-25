package pe.upc.peaceapp.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.upc.peaceapp.identity.api.error.InvalidResetTokenException;
import pe.upc.peaceapp.identity.domain.model.PasswordResetToken;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.PasswordResetTokenRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
    private UserRepository users;
    private PasswordResetTokenRepository tokens;
    private PasswordResetNotifier notifier;
    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(PasswordResetTokenRepository.class);
        notifier = mock(PasswordResetNotifier.class);
        service = new PasswordResetService(users, tokens, encoder, notifier, clock, 30);
        user = new User("ana@peaceapp.pe", encoder.encode("vieja-clave-1"));
        user.setId(UUID.randomUUID());
        user.setLocked(true);
        user.setFailedLoginAttempts(5);
        when(users.findByEmail("ana@peaceapp.pe")).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void requestStoresOnlyTheHashAndNotifiesWithTheRawToken() {
        service.request("ANA@peaceapp.pe");
        var stored = ArgumentCaptor.forClass(PasswordResetToken.class);
        var raw = ArgumentCaptor.forClass(String.class);
        verify(tokens).save(stored.capture());
        verify(notifier).send(anyString(), raw.capture(), any());
        assertThat(stored.getValue().getTokenHash()).hasSize(64).isNotEqualTo(raw.getValue());
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(clock.instant().plusSeconds(30 * 60));
    }

    @Test
    void requestForUnknownEmailDoesNothingVisible() {
        service.request("nadie@peaceapp.pe");
        verifyNoInteractions(notifier);
        verify(tokens, never()).save(any());
    }

    @Test
    void resetChangesPasswordUnlocksAndConsumesToken() {
        service.request("ana@peaceapp.pe");
        var stored = ArgumentCaptor.forClass(PasswordResetToken.class);
        var raw = ArgumentCaptor.forClass(String.class);
        verify(tokens).save(stored.capture());
        verify(notifier).send(anyString(), raw.capture(), any());
        when(tokens.findByTokenHash(stored.getValue().getTokenHash())).thenReturn(Optional.of(stored.getValue()));

        service.reset(raw.getValue(), "nueva-clave-1");

        assertThat(encoder.matches("nueva-clave-1", user.getPasswordHash())).isTrue();
        assertThat(user.isLocked()).isFalse();
        assertThat(stored.getValue().getUsedAt()).isEqualTo(clock.instant());
        assertThatThrownBy(() -> service.reset(raw.getValue(), "otra-clave-12")).isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void resetRejectsUnknownToken() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reset("inventado", "nueva-clave-1")).isInstanceOf(InvalidResetTokenException.class);
    }
}
