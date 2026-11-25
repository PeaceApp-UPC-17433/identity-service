package pe.upc.peaceapp.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.upc.peaceapp.identity.api.dto.LoginResponse;
import pe.upc.peaceapp.identity.application.EmailVerificationService;
import pe.upc.peaceapp.identity.security.JwtService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/email-verification")
public class EmailVerificationController {

    public record ConfirmRequest(@NotBlank @Pattern(regexp = "^\\d{6}$", message = "El codigo tiene 6 digitos") String code) {
    }

    private final EmailVerificationService verification;
    private final JwtService jwtService;

    public EmailVerificationController(EmailVerificationService verification, JwtService jwtService) {
        this.verification = verification;
        this.jwtService = jwtService;
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resend(Authentication authentication) {
        verification.resend(userId(authentication));
    }

    @PostMapping("/confirm")
    public LoginResponse confirm(Authentication authentication, @Valid @RequestBody ConfirmRequest request) {
        return new LoginResponse(jwtService.issueToken(verification.confirm(userId(authentication), request.code())));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
