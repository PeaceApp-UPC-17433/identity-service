package pe.upc.peaceapp.identity.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.SummariesRequest;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.UserLookupResponse;
import pe.upc.peaceapp.identity.api.dto.AccountRequests.UserSummaryResponse;
import pe.upc.peaceapp.identity.application.AccountService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserDirectoryController {

    private final AccountService accountService;

    public UserDirectoryController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<UserLookupResponse> lookup(Authentication authentication, @RequestParam String email) {
        UUID requester = UUID.fromString((String) authentication.getPrincipal());
        return accountService.lookupIdByEmail(requester, email)
                .map(id -> ResponseEntity.ok(new UserLookupResponse(id)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/summaries")
    public java.util.List<UserSummaryResponse> summaries(@Valid @RequestBody SummariesRequest request) {
        return accountService.summaries(request.ids()).stream()
                .map(s -> new UserSummaryResponse(s.id(), s.displayName(), s.avatarUrl())).toList();
    }
}
