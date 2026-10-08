package pe.upc.peaceapp.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.upc.peaceapp.identity.application.AdminService;
import pe.upc.peaceapp.identity.application.error.EmailAlreadyRegisteredException;
import pe.upc.peaceapp.identity.domain.model.Role;

/**
 * Crea el primer ADMINISTRADOR a partir de ADMIN_EMAIL / ADMIN_PASSWORD si aun no existe.
 * Sin el no habria forma de dar de alta cuentas institucionales (US-26).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminService adminService;
    private final IdentityProperties.BootstrapAdmin bootstrapAdmin;

    public AdminBootstrap(AdminService adminService, IdentityProperties properties) {
        this.adminService = adminService;
        this.bootstrapAdmin = properties.bootstrapAdmin();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapAdmin == null || !bootstrapAdmin.isConfigured()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD no configurados: no se crea administrador inicial");
            return;
        }
        try {
            adminService.createVerifiedAccount(bootstrapAdmin.email(), bootstrapAdmin.password(),
                    Role.ADMINISTRADOR, null);
            log.info("Administrador inicial creado: {}", bootstrapAdmin.email());
        } catch (EmailAlreadyRegisteredException e) {
            log.debug("El administrador inicial ya existe");
        }
    }
}
