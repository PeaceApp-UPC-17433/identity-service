package pe.upc.peaceapp.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/** verificationToken solo se incluye en desarrollo (EXPOSE_VERIFICATION_TOKEN=true). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisterResponse(UUID userId, String email, String message, String verificationToken) {
}
