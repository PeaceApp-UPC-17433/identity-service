package pe.upc.peaceapp.identity.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.identity.api.dto.ChangeRoleRequest;
import pe.upc.peaceapp.identity.api.dto.CreateInstitutionalAccountRequest;
import pe.upc.peaceapp.identity.api.dto.UserSummaryResponse;
import pe.upc.peaceapp.identity.application.AdminService;

import java.util.UUID;

/** RNF-12 / US-26: operaciones de administracion. Protegido por rol ADMINISTRADOR en SecurityConfig. */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** US-26: crea una cuenta institucional habilitada para una jurisdiccion. */
    @PostMapping("/institutional-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse createInstitutionalAccount(@Valid @RequestBody CreateInstitutionalAccountRequest request) {
        return UserSummaryResponse.from(adminService.createInstitutionalAccount(
                request.email(), request.password(), request.jurisdiction()));
    }

    /** RNF-12: asigna rol (y jurisdiccion si es institucional) a un usuario existente. */
    @PutMapping("/users/{userId}/role")
    public UserSummaryResponse changeRole(@PathVariable UUID userId, @Valid @RequestBody ChangeRoleRequest request) {
        return UserSummaryResponse.from(adminService.changeRole(userId, request.role(), request.jurisdiction()));
    }

    /** Desbloquea una cuenta antes de que expire el bloqueo temporal. */
    @PostMapping("/users/{userId}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlock(@PathVariable UUID userId) {
        adminService.unlock(userId);
    }
}
