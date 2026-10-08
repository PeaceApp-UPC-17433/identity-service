package pe.upc.peaceapp.identity.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.identity.api.dto.*;
import pe.upc.peaceapp.identity.application.AuthService;
import pe.upc.peaceapp.identity.config.IdentityProperties;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String RESEND_MESSAGE =
            "Si el correo esta registrado y pendiente de verificacion, se envio un nuevo enlace";

    private final AuthService authService;
    private final boolean exposeVerificationToken;

    public AuthController(AuthService authService, IdentityProperties properties) {
        this.authService = authService;
        this.exposeVerificationToken = properties.verification().exposeTokenInResponse();
    }

    /** US-01: registro de usuario. La cuenta queda pendiente de verificacion de correo. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(request.email(), request.password());
        return new RegisterResponse(result.userId(), result.email(),
                "Cuenta creada. Revisa tu correo para verificarla antes de iniciar sesion",
                exposeVerificationToken ? result.verificationToken() : null);
    }

    /** US-01: confirma el correo con el token recibido. */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
    }

    /** US-01: reenvia el correo de verificacion. Responde igual exista o no la cuenta. */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        var token = authService.resendVerification(request.email());
        return new MessageResponse(RESEND_MESSAGE, exposeVerificationToken ? token.orElse(null) : null);
    }

    /** US-02: inicio de sesion. Devuelve access token (JWT) y refresh token. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authService.login(request.email(), request.password()));
    }

    /** RNF-11: renueva la sesion rotando el refresh token. */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(authService.refresh(request.refreshToken()));
    }

    /** Cierra la sesion revocando el refresh token. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
