package pe.upc.peaceapp.identity.domain.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upc.peaceapp.identity.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationTokenHash(String verificationTokenHash);

    /** Carga las zonas de interes en la misma consulta (evita LazyInitializationException y N+1). */
    @EntityGraph(attributePaths = "interestZones")
    Optional<User> findWithInterestZonesById(UUID id);
}
