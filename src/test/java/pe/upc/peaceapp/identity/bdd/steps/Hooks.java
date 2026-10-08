package pe.upc.peaceapp.identity.bdd.steps;

import io.cucumber.java.Before;
import pe.upc.peaceapp.identity.domain.repository.OutboxEventRepository;
import pe.upc.peaceapp.identity.domain.repository.RefreshTokenRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

public class Hooks {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OutboxEventRepository outboxEventRepository;

    public Hooks(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                 OutboxEventRepository outboxEventRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    /** Cada escenario arranca con la base vacia. Las zonas de interes se borran en cascada con el usuario. */
    @Before
    public void limpiarBase() {
        refreshTokenRepository.deleteAll();
        outboxEventRepository.deleteAll();
        userRepository.deleteAll();
    }
}
