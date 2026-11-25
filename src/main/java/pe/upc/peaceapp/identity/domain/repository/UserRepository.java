package pe.upc.peaceapp.identity.domain.repository;

import pe.upc.peaceapp.identity.domain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Override
    @EntityGraph(attributePaths = "interestZones")
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
