package pe.upc.peaceapp.identity.api.dto;

import pe.upc.peaceapp.identity.application.AuthService.AuthTokens;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {
    public static TokenResponse from(AuthTokens tokens) {
        return new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresInSeconds(), tokens.refreshToken());
    }
}
