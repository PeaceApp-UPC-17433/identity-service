package pe.upc.peaceapp.identity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.upc.peaceapp.identity.application.PasswordResetNotifier;

import java.time.Instant;

@Component
@Profile({"local", "dev", "test"})
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void send(String email, String token, Instant expiresAt) {
        log.info("Recuperacion de contrasena: token={} expira={}", token, expiresAt);
    }
}
