package pe.upc.peaceapp.identity.api;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.ChangeRoleRequest;
import pe.upc.peaceapp.identity.api.dto.UserProfileResponse;
import pe.upc.peaceapp.identity.application.AccountService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUsersController {

    private final AccountService accountService;

    public AdminUsersController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("/{id}/role")
    public UserProfileResponse changeRole(Authentication authentication, @PathVariable UUID id,
                                          @Valid @RequestBody ChangeRoleRequest request) {
        UUID admin = UUID.fromString((String) authentication.getPrincipal());
        return UserProfileResponse.from(accountService.changeRole(admin, id, request.role()));
    }
}
