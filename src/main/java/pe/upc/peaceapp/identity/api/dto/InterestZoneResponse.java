package pe.upc.peaceapp.identity.api.dto;

import pe.upc.peaceapp.identity.domain.model.InterestZone;

import java.util.UUID;

public record InterestZoneResponse(UUID id, String label, double latitude, double longitude, int radiusMeters) {
    public static InterestZoneResponse from(InterestZone zone) {
        return new InterestZoneResponse(zone.getId(), zone.getLabel(), zone.getLatitude(),
                zone.getLongitude(), zone.getRadiusMeters());
    }
}
