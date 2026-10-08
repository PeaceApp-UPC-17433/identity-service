package pe.upc.peaceapp.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** US-26: alta de cuenta institucional para una jurisdiccion (p. ej. distrito "Miraflores"). */
public record CreateInstitutionalAccountRequest(
        @Schema(example = "miraflores@muni.gob.pe") @NotBlank @Email @Size(max = 254) String email,
        @Schema(example = "Clave12345")
        @NotBlank
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String password,
        @Schema(example = "Miraflores") @NotBlank @Size(max = 100) String jurisdiction
) {
}
