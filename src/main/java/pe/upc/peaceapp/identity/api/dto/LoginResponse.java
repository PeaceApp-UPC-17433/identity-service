package pe.upc.peaceapp.identity.api.dto;

public record LoginResponse(String accessToken, String tokenType) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
