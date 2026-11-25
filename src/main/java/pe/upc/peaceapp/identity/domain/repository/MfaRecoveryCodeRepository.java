package pe.upc.peaceapp.identity.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upc.peaceapp.identity.domain.model.MfaRecoveryCode;

import java.util.List;
import java.util.UUID;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {
    List<MfaRecoveryCode> findByUserIdAndUsedAtIsNull(UUID userId);

    void deleteByUserId(UUID userId);

    long countByUserIdAndUsedAtIsNull(UUID userId);
}
