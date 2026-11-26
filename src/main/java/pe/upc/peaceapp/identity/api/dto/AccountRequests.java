package pe.upc.peaceapp.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.upc.peaceapp.identity.domain.model.Role;

public final class AccountRequests {
    private AccountRequests() {
    }

    public record UpdateProfileRequest(
            @Size(max = 50) String firstName,
            @Size(max = 50) String lastName,
            @Size(max = 20) String phoneNumber,
            @Size(max = 500) String avatarUrl,
            // Opcional (null = no cambia), pero si viene debe ser un correo real: "" dejaba la cuenta sin correo.
            @Email @Pattern(regexp = EmailPolicy.REGEX, message = EmailPolicy.MESSAGE) @Size(max = 120) String email) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String newPassword) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String newPassword) {
    }

    public record ChangeRoleRequest(@NotNull Role role) {
    }

    public record UserLookupResponse(java.util.UUID id) {
    }

    public record SummariesRequest(@jakarta.validation.constraints.NotEmpty @Size(max = 100) java.util.List<java.util.UUID> ids) {
    }

    public record UserSummaryResponse(java.util.UUID id, String displayName, String avatarUrl) {
    }
}
