package pe.upc.peaceapp.identity.api.dto;

import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        Role role,
        String firstName,
        String lastName,
        String phoneNumber,
        String avatarUrl,
        String jurisdiction,
        int reputationScore,
        List<InterestZoneResponse> interestZones
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getJurisdiction(),
                user.getReputationScore(),
                user.getInterestZones().stream().map(InterestZoneResponse::from).toList());
    }
}
