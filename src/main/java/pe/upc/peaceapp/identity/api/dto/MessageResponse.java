package pe.upc.peaceapp.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(String message, String verificationToken) {
    public MessageResponse(String message) {
        this(message, null);
    }
}
