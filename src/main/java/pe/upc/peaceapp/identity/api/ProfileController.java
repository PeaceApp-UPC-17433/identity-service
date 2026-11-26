package pe.upc.peaceapp.identity.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.identity.api.dto.InterestZoneRequest;
import pe.upc.peaceapp.identity.api.dto.InterestZoneResponse;
import pe.upc.peaceapp.identity.api.dto.UserProfileResponse;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.ChangePasswordRequest;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.UpdateProfileRequest;
import pe.upc.peaceapp.identity.application.AccountService;
import pe.upc.peaceapp.identity.application.ProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {

    private final ProfileService profileService;
    private final AccountService accountService;

    public ProfileController(ProfileService profileService, AccountService accountService) {
        this.profileService = profileService;
        this.accountService = accountService;
    }

    /** US-03 / US-05: ver perfil, zonas de interes y reputacion propias. */
    @GetMapping
    public UserProfileResponse getProfile(Authentication authentication) {
        return UserProfileResponse.from(profileService.getById(currentUserId(authentication)));
    }

    /** US-03: agregar una zona de interes al perfil. */
    @PostMapping("/interest-zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InterestZoneResponse> addInterestZone(Authentication authentication,
                                                                  @Valid @RequestBody InterestZoneRequest request) {
        var zone = profileService.addInterestZone(currentUserId(authentication),
                request.label(), request.latitude(), request.longitude(), request.radiusMeters());
        return ResponseEntity.status(HttpStatus.CREATED).body(InterestZoneResponse.from(zone));
    }

    @PutMapping
    public UserProfileResponse updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return UserProfileResponse.from(accountService.updateProfile(currentUserId(authentication), request));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(currentUserId(authentication), request.currentPassword(), request.newPassword());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication authentication) {
        accountService.deleteAccount(currentUserId(authentication));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
