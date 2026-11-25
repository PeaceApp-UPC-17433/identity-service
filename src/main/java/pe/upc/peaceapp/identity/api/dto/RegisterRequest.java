package pe.upc.peaceapp.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres") String password,
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        @Size(max = 20) String phoneNumber
) {
}
