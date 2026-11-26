package pe.upc.peaceapp.identity.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upc.peaceapp.identity.domain.model.EmailVerification;

import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
}
