package pe.upc.peaceapp.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.identity.application.mfa.MfaService;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;
import pe.upc.peaceapp.identity.security.JwtService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/mfa")
public class MfaController {

    public record CodeRequest(@NotBlank @Size(max = 20) String code) {
    }

    public record ConfirmIdentityRequest(@NotBlank @Size(max = 72) String password, @NotBlank @Size(max = 20) String code) {
    }

    public record StatusResponse(boolean enabled, long recoveryCodesRemaining) {
    }

    public record EnabledResponse(String accessToken, List<String> recoveryCodes) {
    }

    public record RecoveryCodesResponse(List<String> recoveryCodes) {
    }

    private final MfaService mfa;
    private final JwtService jwtService;
    private final UserRepository users;

    public MfaController(MfaService mfa, JwtService jwtService, UserRepository users) {
        this.mfa = mfa;
        this.jwtService = jwtService;
        this.users = users;
    }

    @GetMapping
    public StatusResponse status(Authentication authentication) {
        UUID id = userId(authentication);
        boolean enabled = users.findById(id).orElseThrow().isMfaEnabled();
        return new StatusResponse(enabled, enabled ? mfa.remainingRecoveryCodes(id) : 0);
    }

    @PostMapping("/setup")
    public MfaService.Setup setup(Authentication authentication) {
        return mfa.setup(userId(authentication));
    }

    @PostMapping("/enable")
    public EnabledResponse enable(Authentication authentication, @Valid @RequestBody CodeRequest request) {
        UUID id = userId(authentication);
        List<String> codes = mfa.enable(id, request.code());
        return new EnabledResponse(jwtService.issueToken(users.findById(id).orElseThrow(), true), codes);
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(Authentication authentication, @Valid @RequestBody ConfirmIdentityRequest request) {
        mfa.disable(userId(authentication), request.password(), request.code());
    }

    @PostMapping("/recovery-codes")
    public RecoveryCodesResponse regenerate(Authentication authentication, @Valid @RequestBody ConfirmIdentityRequest request) {
        return new RecoveryCodesResponse(mfa.regenerateRecoveryCodes(userId(authentication), request.password(), request.code()));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
