package pe.upc.peaceapp.identity.api.dto;

import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;

import java.util.UUID;

public record UserSummaryResponse(UUID id, String email, Role role, String jurisdiction) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole(), user.getJurisdiction());
    }
}
