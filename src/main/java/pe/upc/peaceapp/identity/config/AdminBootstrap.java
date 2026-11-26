package pe.upc.peaceapp.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.application.AuthService;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${identity.bootstrap-admin.email:}") String email,
                          @Value("${identity.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        if (users.findAll().stream().anyMatch(u -> u.getRole() == Role.ADMINISTRADOR)) return;
        String normalized = AuthService.normalizeEmail(email);
        User admin = users.findByEmail(normalized).orElseGet(() -> new User(normalized, passwordEncoder.encode(password)));
        admin.setRole(Role.ADMINISTRADOR);
        admin.setEmailVerified(true);
        users.save(admin);
        log.info("Administrador inicial creado userId={}", admin.getId());
    }
}
