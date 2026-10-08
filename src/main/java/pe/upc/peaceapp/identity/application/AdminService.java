package pe.upc.peaceapp.identity.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.application.error.EmailAlreadyRegisteredException;
import pe.upc.peaceapp.identity.domain.event.IdentityEvent;
import pe.upc.peaceapp.identity.domain.exception.ResourceNotFoundException;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import java.time.Clock;
import java.util.UUID;

/** RNF-12 / US-26: administracion de cuentas, roles y jurisdicciones. Solo para ADMINISTRADOR. */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventOutbox eventOutbox;
    private final Clock clock;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        EventOutbox eventOutbox, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventOutbox = eventOutbox;
        this.clock = clock;
    }

    /** US-26: alta de una cuenta institucional habilitada para una jurisdiccion. */
    @Transactional
    public User createInstitutionalAccount(String email, String rawPassword, String jurisdiction) {
        return createVerifiedAccount(email, rawPassword, Role.INSTITUCIONAL, jurisdiction.trim());
    }

    @Transactional
    public User createVerifiedAccount(String rawEmail, String rawPassword, Role role, String jurisdiction) {
        String email = EmailAddress.normalize(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = User.createVerified(email, passwordEncoder.encode(rawPassword), role, jurisdiction,
                clock.instant());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException();
        }
        eventOutbox.append(new IdentityEvent.UserRoleChanged(user.getId(), role.name(), user.getJurisdiction()));
        return user;
    }

    @Transactional
    public User changeRole(UUID userId, Role role, String jurisdiction) {
        User user = find(userId);
        user.changeRole(role, jurisdiction == null ? null : jurisdiction.trim());
        eventOutbox.append(new IdentityEvent.UserRoleChanged(user.getId(), role.name(), user.getJurisdiction()));
        return user;
    }

    @Transactional
    public void unlock(UUID userId) {
        find(userId).unlock();
    }

    private User find(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}
