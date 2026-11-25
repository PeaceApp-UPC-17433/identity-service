package pe.upc.peaceapp.identity.application;

import java.time.Instant;

public interface PasswordResetNotifier {
    void send(String email, String token, Instant expiresAt);
}
