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
import pe.upc.peaceapp.identity.messaging.EmailNotifications;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
    private final CapturingPublisher publisher = new CapturingPublisher();
    private UserRepository users;
    private PasswordResetTokenRepository tokens;
    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(PasswordResetTokenRepository.class);
        service = new PasswordResetService(users, tokens, encoder, new EmailNotifications(publisher), clock,
                "http://localhost:3000/", 30);
        user = new User("ana@peaceapp.pe", encoder.encode("Vieja-clave-1"));
        user.setId(UUID.randomUUID());
        user.setLocale("en");
        user.setLocked(true);
        user.setFailedLoginAttempts(5);
        when(users.findByEmail("ana@peaceapp.pe")).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private String tokenFromEvent() {
        var url = publisher.events.get(0).params().get("resetUrl");
        return url.substring(url.indexOf("token=") + 6);
    }

    @Test
    void requestStoresOnlyTheHashAndPublishesTheLinkInTheUsersLanguage() {
        service.request("ANA@peaceapp.pe");
        var stored = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(stored.capture());
        var event = publisher.events.get(0);
        assertThat(event.routingKey()).isEqualTo("notification.email-requested");
        assertThat(event.template()).isEqualTo("PASSWORD_RESET");
        assertThat(event.payload().get("to")).isEqualTo("ana@peaceapp.pe");
        assertThat(event.payload().get("locale")).isEqualTo("en");
        assertThat(event.params().get("resetUrl")).startsWith("http://localhost:3000/reset-password?token=");
        assertThat(event.params().get("expiresMinutes")).isEqualTo("30");
        assertThat(stored.getValue().getTokenHash()).hasSize(64).isNotEqualTo(tokenFromEvent());
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(clock.instant().plusSeconds(30 * 60));
    }

    @Test
    void requestForUnknownEmailPublishesNothing() {
        service.request("nadie@peaceapp.pe");
        assertThat(publisher.events).isEmpty();
        verify(tokens, never()).save(any());
    }

    @Test
    void resetChangesPasswordUnlocksConsumesTheTokenAndNotifies() {
        service.request("ana@peaceapp.pe");
        var stored = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(stored.capture());
        when(tokens.findByTokenHash(stored.getValue().getTokenHash())).thenReturn(Optional.of(stored.getValue()));
        String token = tokenFromEvent();

        service.reset(token, "Nueva-clave-1");

        assertThat(encoder.matches("Nueva-clave-1", user.getPasswordHash())).isTrue();
        assertThat(user.isLocked()).isFalse();
        assertThat(stored.getValue().getUsedAt()).isEqualTo(clock.instant());
        assertThat(publisher.events.get(1).template()).isEqualTo("PASSWORD_CHANGED");
        assertThatThrownBy(() -> service.reset(token, "Otra-clave-12")).isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void resetRejectsUnknownToken() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reset("inventado", "Nueva-clave-1")).isInstanceOf(InvalidResetTokenException.class);
    }
}
