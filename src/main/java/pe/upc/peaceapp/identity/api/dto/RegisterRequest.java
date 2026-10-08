package pe.upc.peaceapp.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * US-01: registro de usuario. Politica de contrasena: 8 a 72 caracteres (limite de BCrypt),
 * con al menos una letra y un numero.
 */
public record RegisterRequest(
        @Schema(example = "ana@peaceapp.pe") @NotBlank @Email @Size(max = 254) String email,
        @Schema(example = "Clave12345", description = "8 a 72 caracteres, al menos una letra y un numero")
        @NotBlank
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String password
) {
}
