package pe.upc.peaceapp.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.UpdateProfileRequest;
import pe.upc.peaceapp.identity.api.error.CurrentPasswordMismatchException;
import pe.upc.peaceapp.identity.api.error.EmailAlreadyRegisteredException;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = find(userId);
        if (request.firstName() != null) user.setFirstName(trimToNull(request.firstName()));
        if (request.lastName() != null) user.setLastName(trimToNull(request.lastName()));
        if (request.phoneNumber() != null) user.setPhoneNumber(trimToNull(request.phoneNumber()));
        if (request.avatarUrl() != null) user.setAvatarUrl(trimToNull(request.avatarUrl()));
        if (request.email() != null) {
            String email = AuthService.normalizeEmail(request.email());
            if (!email.equals(user.getEmail())) {
                if (users.existsByEmail(email)) throw new EmailAlreadyRegisteredException(email);
                user.setEmail(email);
                user.setEmailVerified(false);
                log.info("Correo actualizado userId={}", userId);
            }
        }
        return users.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = find(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.info("Cambio de contrasena rechazado: contrasena actual incorrecta userId={}", userId);
            throw new CurrentPasswordMismatchException();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        log.info("Contrasena actualizada userId={}", userId);
    }

    public Optional<UUID> lookupIdByEmail(UUID requesterId, String email) {
        Optional<UUID> found = users.findByEmail(AuthService.normalizeEmail(email)).map(User::getId);
        log.info("Busqueda de usuario por correo requesterId={} encontrado={}", requesterId, found.isPresent());
        return found;
    }

    @Transactional
    public User changeRole(UUID adminId, UUID targetId, Role role) {
        User user = find(targetId);
        Role previous = user.getRole();
        user.setRole(role);
        log.info("Rol cambiado adminId={} userId={} de={} a={}", adminId, targetId, previous, role);
        return users.save(user);
    }

    public List<UserSummary> summaries(List<UUID> ids) {
        return users.findAllById(ids).stream().map(AccountService::summaryOf).toList();
    }

    public record UserSummary(UUID id, String displayName, String avatarUrl) {
    }

    private static UserSummary summaryOf(User user) {
        if (user.getDeletedAt() != null) return new UserSummary(user.getId(), "Usuario eliminado", null);
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null || user.getLastName().isBlank() ? "" : " " + user.getLastName().charAt(0) + ".";
        String name = (first + last).trim();
        return new UserSummary(user.getId(), name.isEmpty() ? "Vecino" : name, user.getAvatarUrl());
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = find(userId);
        user.setEmail("deleted-" + userId + "@deleted.invalid");
        user.setFirstName(null);
        user.setLastName(null);
        user.setPhoneNumber(null);
        user.setAvatarUrl(null);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setLocked(true);
        user.setDeletedAt(java.time.Instant.now());
        user.getInterestZones().clear();
        users.save(user);
        log.info("Cuenta eliminada userId={}", userId);
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
