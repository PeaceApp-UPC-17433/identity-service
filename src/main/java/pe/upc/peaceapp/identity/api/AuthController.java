package pe.upc.peaceapp.identity.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.identity.api.dto.LoginRequest;
import pe.upc.peaceapp.identity.api.dto.LoginResponse;
import pe.upc.peaceapp.identity.api.dto.RegisterRequest;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.ForgotPasswordRequest;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.ResetPasswordRequest;
import pe.upc.peaceapp.identity.api.error.MfaExceptions.InvalidChallengeException;
import pe.upc.peaceapp.identity.application.AuthService;
import pe.upc.peaceapp.identity.application.mfa.MfaService;
import pe.upc.peaceapp.identity.security.JwtService;
import pe.upc.peaceapp.identity.application.PasswordResetService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final MfaService mfaService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService, MfaService mfaService,
                          JwtService jwtService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.mfaService = mfaService;
        this.jwtService = jwtService;
    }

    /** US-01: registro de usuario. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password(), request.firstName(), request.lastName(),
                request.phoneNumber(), request.locale());
    }

    /** US-02: inicio de sesion. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    public record MfaLoginRequest(@jakarta.validation.constraints.NotBlank String mfaToken,
                                  @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 20) String code) {
    }

    @PostMapping("/login/mfa")
    public ResponseEntity<LoginResponse> loginMfa(@Valid @RequestBody MfaLoginRequest request) {
        java.util.UUID userId;
        try {
            userId = jwtService.parseMfaChallenge(request.mfaToken());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new InvalidChallengeException();
        }
        var user = mfaService.verifyLogin(userId, request.code());
        return ResponseEntity.ok(new LoginResponse(jwtService.issueToken(user, true)));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.request(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
    }
}
