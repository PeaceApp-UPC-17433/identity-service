package pe.upc.peaceapp.identity.domain.repository;

import pe.upc.peaceapp.identity.domain.model.InterestZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterestZoneRepository extends JpaRepository<InterestZone, UUID> {
    List<InterestZone> findByUserId(UUID userId);
}
