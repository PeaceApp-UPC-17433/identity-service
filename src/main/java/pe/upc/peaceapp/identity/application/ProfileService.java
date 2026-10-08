package pe.upc.peaceapp.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.domain.model.InterestZone;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.InterestZoneRepository;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import java.util.NoSuchElementException;
import java.util.UUID;

/** US-03: gestion de perfil y zonas de interes. */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final InterestZoneRepository interestZoneRepository;

    public ProfileService(UserRepository userRepository, InterestZoneRepository interestZoneRepository) {
        this.userRepository = userRepository;
        this.interestZoneRepository = interestZoneRepository;
    }

    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + userId));
    }

    @Transactional
    public InterestZone addInterestZone(UUID userId, String label, double latitude, double longitude, int radiusMeters) {
        User user = getById(userId);
        InterestZone zone = new InterestZone(user, label, latitude, longitude, radiusMeters);
        return interestZoneRepository.save(zone);
    }
}
