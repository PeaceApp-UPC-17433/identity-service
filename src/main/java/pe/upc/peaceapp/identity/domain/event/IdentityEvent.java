package pe.upc.peaceapp.identity.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Eventos de dominio publicados por BC-01 (seccion 2.4 del informe). */
public sealed interface IdentityEvent {

    UUID userId();

    default String type() {
        return getClass().getSimpleName();
    }

    /**
     * US-01. Incluye el token de verificacion porque notification-service (BC-08) es quien envia el correo
     * con el enlace; identity-service solo guarda su hash.
     */
    record UserRegistered(UUID userId, String email, String verificationToken, Instant tokenExpiresAt)
            implements IdentityEvent {
    }

    /** US-01: reenvio del correo de verificacion. */
    record VerificationRequested(UUID userId, String email, String verificationToken, Instant tokenExpiresAt)
            implements IdentityEvent {
    }

    record UserVerified(UUID userId, String email) implements IdentityEvent {
    }

    /** US-03: consumido por notification-service para el resumen por zona (US-25). */
    record InterestZoneUpdated(UUID userId, UUID zoneId, Action action, String label,
                               double latitude, double longitude, int radiusMeters) implements IdentityEvent {
        public enum Action { ADDED, REMOVED }
    }

    /** RNF-11 / QAS-06: bloqueo por intentos fallidos. */
    record AccountLocked(UUID userId, Instant lockedUntil) implements IdentityEvent {
    }

    /** RNF-12 / US-26: cambio de rol o jurisdiccion. */
    record UserRoleChanged(UUID userId, String role, String jurisdiction) implements IdentityEvent {
    }
}
