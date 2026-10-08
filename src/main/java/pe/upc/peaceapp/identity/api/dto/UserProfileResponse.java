package pe.upc.peaceapp.identity.api.dto;

import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;

import java.util.List;
import java.util.UUID;

/** US-03 / US-05: perfil del usuario y su reputacion. */
public record UserProfileResponse(
        UUID id,
        String email,
        Role role,
        String jurisdiction,
        int reputationScore,
        List<InterestZoneResponse> interestZones
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getJurisdiction(),
                user.getReputationScore(),
                user.getInterestZones().stream().map(InterestZoneResponse::from).toList());
    }
}
