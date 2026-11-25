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
import pe.upc.peaceapp.identity.application.AuthService;
import pe.upc.peaceapp.identity.application.PasswordResetService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    /** US-01: registro de usuario. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password(), request.firstName(), request.lastName(),
                request.phoneNumber());
    }

    /** US-02: inicio de sesion. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
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
