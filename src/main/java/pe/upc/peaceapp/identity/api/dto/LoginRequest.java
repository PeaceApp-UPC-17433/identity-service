package pe.upc.peaceapp.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

/** US-02: inicio de sesion. */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
