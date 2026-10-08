package pe.upc.peaceapp.identity.api.dto;

import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;

import java.util.List;
import java.util.UUID;

/** US-03: perfil del usuario. reputationScore es una cache del valor que calcula validation-service. */
public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        Role role,
        String jurisdiction,
        boolean emailVerified,
        int reputationScore,
        List<InterestZoneResponse> interestZones
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getJurisdiction(),
                user.isEmailVerified(),
                user.getReputationScore(),
                user.getInterestZones().stream().map(InterestZoneResponse::from).toList());
    }
}
