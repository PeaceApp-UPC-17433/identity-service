package pe.upc.peaceapp.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** US-03: zona de interes del perfil. Radio entre 100 m y 5 km. */
public record InterestZoneRequest(
        @Schema(example = "Casa") @NotBlank @Size(max = 60) String label,
        @Schema(example = "-12.1211") @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @Schema(example = "-77.0297") @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Schema(example = "500") @NotNull @Min(100) @Max(5000) Integer radiusMeters
) {
}
