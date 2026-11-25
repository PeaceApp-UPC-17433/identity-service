package pe.upc.peaceapp.identity.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upc.peaceapp.identity.domain.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
