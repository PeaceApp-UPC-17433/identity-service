package pe.upc.peaceapp.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.UpdateProfileRequest;
import pe.upc.peaceapp.identity.api.error.CurrentPasswordMismatchException;
import pe.upc.peaceapp.identity.api.error.EmailAlreadyRegisteredException;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountServiceTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserRepository users;
    private CapturingPublisher publisher;
    private EmailVerificationService verification;
    private AccountService service;
    private User user;
    private UUID id;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        publisher = new CapturingPublisher();
        verification = mock(EmailVerificationService.class);
        service = new AccountService(users, encoder, new pe.upc.peaceapp.identity.messaging.EmailNotifications(publisher), verification);
        id = UUID.randomUUID();
        user = new User("ana@peaceapp.pe", encoder.encode("clave-actual-1"));
        user.setId(id);
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void updateProfileChangesOnlyProvidedFields() {
        service.updateProfile(id, new UpdateProfileRequest("Ana", null, " 999 ", null, null));
        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isNull();
        assertThat(user.getPhoneNumber()).isEqualTo("999");
    }

    @Test
    void updateProfileRejectsEmailAlreadyInUse() {
        when(users.existsByEmail("otra@peaceapp.pe")).thenReturn(true);
        assertThatThrownBy(() -> service.updateProfile(id, new UpdateProfileRequest(null, null, null, null, "Otra@PeaceApp.pe")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        assertThatThrownBy(() -> service.changePassword(id, "incorrecta", "nueva-clave-1"))
                .isInstanceOf(CurrentPasswordMismatchException.class);
        service.changePassword(id, "clave-actual-1", "nueva-clave-1");
        assertThat(encoder.matches("nueva-clave-1", user.getPasswordHash())).isTrue();
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).template()).isEqualTo("PASSWORD_CHANGED");
    }

    @Test
    void changeRoleUpdatesUser() {
        assertThat(service.changeRole(UUID.randomUUID(), id, Role.MODERADOR).getRole()).isEqualTo(Role.MODERADOR);
    }

    @Test
    void lookupNormalizesEmail() {
        when(users.findByEmail("ana@peaceapp.pe")).thenReturn(Optional.of(user));
        assertThat(service.lookupIdByEmail(UUID.randomUUID(), " Ana@PeaceApp.pe ")).contains(id);
    }

    @Test
    void summariesShowFirstNameAndLastInitialOnly() {
        user.setFirstName("Ana");
        user.setLastName("Quispe");
        when(users.findAllById(java.util.List.of(id))).thenReturn(java.util.List.of(user));
        var summary = service.summaries(java.util.List.of(id)).get(0);
        assertThat(summary.displayName()).isEqualTo("Ana Q.");
    }

    @Test
    void deleteAccountAnonymizesAndLocksTheUser() {
        user.setFirstName("Ana");
        service.deleteAccount(id);
        assertThat(user.getEmail()).endsWith("@deleted.invalid");
        assertThat(user.getFirstName()).isNull();
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        when(users.findAllById(java.util.List.of(id))).thenReturn(java.util.List.of(user));
        assertThat(service.summaries(java.util.List.of(id)).get(0).displayName()).isEqualTo("Usuario eliminado");
    }

    @Test
    void changingTheEmailMarksItUnverifiedAndIssuesANewCode() {
        user.setEmailVerified(true);
        service.updateProfile(id, new UpdateProfileRequest(null, null, null, null, "Nuevo@PeaceApp.pe"));
        assertThat(user.getEmail()).isEqualTo("nuevo@peaceapp.pe");
        assertThat(user.isEmailVerified()).isFalse();
        org.mockito.Mockito.verify(verification).issue(user);
    }
}
