package pe.upc.peaceapp.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** US-02: inicio de sesion. */
public record LoginRequest(
        @Schema(example = "admin@peaceapp.pe") @NotBlank String email,
        @Schema(example = "Admin12345") @NotBlank String password
) {
}
