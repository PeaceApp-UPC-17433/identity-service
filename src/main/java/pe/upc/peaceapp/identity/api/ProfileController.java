package pe.upc.peaceapp.identity.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.identity.api.dto.InterestZoneRequest;
import pe.upc.peaceapp.identity.api.dto.InterestZoneResponse;
import pe.upc.peaceapp.identity.api.dto.UpdateProfileRequest;
import pe.upc.peaceapp.identity.api.dto.UserProfileResponse;
import pe.upc.peaceapp.identity.application.ProfileService;

import java.util.List;
import java.util.UUID;

/** US-03: perfil propio y zonas de interes. */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/users/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserProfileResponse getProfile(@AuthenticationPrincipal UUID userId) {
        return UserProfileResponse.from(profileService.getProfile(userId));
    }

    @PatchMapping
    public UserProfileResponse updateProfile(@AuthenticationPrincipal UUID userId,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        return UserProfileResponse.from(profileService.updateProfile(userId, request.displayName()));
    }

    @GetMapping("/interest-zones")
    public List<InterestZoneResponse> listInterestZones(@AuthenticationPrincipal UUID userId) {
        return profileService.listInterestZones(userId).stream().map(InterestZoneResponse::from).toList();
    }

    @PostMapping("/interest-zones")
    @ResponseStatus(HttpStatus.CREATED)
    public InterestZoneResponse addInterestZone(@AuthenticationPrincipal UUID userId,
                                                @Valid @RequestBody InterestZoneRequest request) {
        var zone = profileService.addInterestZone(userId, request.label(), request.latitude(),
                request.longitude(), request.radiusMeters());
        return InterestZoneResponse.from(zone);
    }

    @DeleteMapping("/interest-zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeInterestZone(@AuthenticationPrincipal UUID userId, @PathVariable UUID zoneId) {
        profileService.removeInterestZone(userId, zoneId);
    }
}
