package pe.upc.peaceapp.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.config.IdentityProperties;
import pe.upc.peaceapp.identity.domain.event.IdentityEvent;
import pe.upc.peaceapp.identity.domain.event.IdentityEvent.InterestZoneUpdated.Action;
import pe.upc.peaceapp.identity.domain.exception.ResourceNotFoundException;
import pe.upc.peaceapp.identity.domain.model.InterestZone;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** US-03: gestion de perfil y zonas de interes. */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final EventOutbox eventOutbox;
    private final IdentityProperties properties;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, EventOutbox eventOutbox,
                          IdentityProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.eventOutbox = eventOutbox;
        this.properties = properties;
        this.clock = clock;
    }

    /** Devuelve el usuario con sus zonas ya cargadas, listo para mapear fuera de la transaccion. */
    @Transactional(readOnly = true)
    public User getProfile(UUID userId) {
        return loadWithZones(userId);
    }

    @Transactional
    public User updateProfile(UUID userId, String displayName) {
        User user = loadWithZones(userId);
        user.updateProfile(displayName == null || displayName.isBlank() ? null : displayName.trim());
        return user;
    }

    @Transactional(readOnly = true)
    public List<InterestZone> listInterestZones(UUID userId) {
        return List.copyOf(loadWithZones(userId).getInterestZones());
    }

    @Transactional
    public InterestZone addInterestZone(UUID userId, String label, double latitude, double longitude,
                                        int radiusMeters) {
        User user = loadWithZones(userId);
        InterestZone zone = user.addInterestZone(label.trim(), latitude, longitude, radiusMeters,
                properties.profile().maxInterestZones(), clock.instant());
        eventOutbox.append(zoneEvent(userId, zone, Action.ADDED));
        return zone;
    }

    @Transactional
    public void removeInterestZone(UUID userId, UUID zoneId) {
        InterestZone zone = loadWithZones(userId).removeInterestZone(zoneId);
        eventOutbox.append(zoneEvent(userId, zone, Action.REMOVED));
    }

    private User loadWithZones(UUID userId) {
        return userRepository.findWithInterestZonesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static IdentityEvent zoneEvent(UUID userId, InterestZone zone, Action action) {
        return new IdentityEvent.InterestZoneUpdated(userId, zone.getId(), action, zone.getLabel(),
                zone.getLatitude(), zone.getLongitude(), zone.getRadiusMeters());
    }
}
