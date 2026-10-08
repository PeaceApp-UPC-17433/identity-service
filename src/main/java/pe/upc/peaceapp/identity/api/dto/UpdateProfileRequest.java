package pe.upc.peaceapp.identity.api.dto;

import jakarta.validation.constraints.Size;

/** US-03: datos editables del perfil. */
public record UpdateProfileRequest(@Size(max = 80) String displayName) {
}
