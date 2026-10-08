package pe.upc.peaceapp.identity.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.upc.peaceapp.identity.domain.model.Role;

/** RNF-12: jurisdiction es obligatoria solo cuando role = INSTITUCIONAL. */
public record ChangeRoleRequest(@NotNull Role role, @Size(max = 100) String jurisdiction) {
}
