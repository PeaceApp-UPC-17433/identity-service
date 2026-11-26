package pe.upc.peaceapp.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String accessToken, String tokenType, Boolean mfaRequired, String mfaToken) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer", null, null);
    }

    public static LoginResponse challenge(String mfaToken) {
        return new LoginResponse(null, "Bearer", true, mfaToken);
    }
}
