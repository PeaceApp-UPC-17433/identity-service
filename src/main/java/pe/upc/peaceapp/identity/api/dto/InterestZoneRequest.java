package pe.upc.peaceapp.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** US-03: zona de interes del perfil. */
public record InterestZoneRequest(
        @NotBlank String label,
        double latitude,
        double longitude,
        @Positive int radiusMeters
) {
}
