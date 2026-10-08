package pe.upc.peaceapp.identity.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pe.upc.peaceapp.identity.domain.exception.DomainRuleViolationException;
import pe.upc.peaceapp.identity.domain.exception.ResourceNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Aggregate root de BC-01: cuenta, credenciales, rol, jurisdiccion y zonas de interes. */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** US-26: jurisdiccion asignada a cuentas institucionales. Null para los demas roles. */
    @Column(length = 100)
    private String jurisdiction;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** Cache local de la reputacion calculada por validation-service (evento ReputationChanged). */
    @Column(name = "reputation_score", nullable = false)
    private int reputationScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<InterestZone> interestZones = new ArrayList<>();

    private User(String email, String passwordHash, Role role, String jurisdiction, boolean emailVerified, Instant now) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.jurisdiction = jurisdiction;
        this.emailVerified = emailVerified;
        this.createdAt = now;
        validateJurisdiction(role, jurisdiction);
    }

    /** US-01: un ciudadano se registra sin verificar y recibe un token de verificacion. */
    public static User registerCitizen(String email, String passwordHash,
                                       String verificationTokenHash, Instant tokenExpiresAt, Instant now) {
        User user = new User(email, passwordHash, Role.CIUDADANO, null, false, now);
        user.issueVerificationToken(verificationTokenHash, tokenExpiresAt);
        return user;
    }

    /** Cuentas creadas por un administrador (US-26) o por el bootstrap: ya verificadas. */
    public static User createVerified(String email, String passwordHash, Role role, String jurisdiction, Instant now) {
        return new User(email, passwordHash, role, jurisdiction, true, now);
    }

    public List<InterestZone> getInterestZones() {
        return Collections.unmodifiableList(interestZones);
    }

    // ---- Verificacion de correo (US-01) ----

    public void issueVerificationToken(String tokenHash, Instant expiresAt) {
        if (emailVerified) {
            throw new DomainRuleViolationException("El correo ya esta verificado");
        }
        this.verificationTokenHash = tokenHash;
        this.verificationTokenExpiresAt = expiresAt;
    }

    public boolean isVerificationTokenExpired(Instant now) {
        return verificationTokenExpiresAt == null || !now.isBefore(verificationTokenExpiresAt);
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.verificationTokenHash = null;
        this.verificationTokenExpiresAt = null;
    }

    // ---- Inicio de sesion y bloqueo (US-02, RNF-11) ----

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Registra un intento fallido. Al llegar al maximo bloquea la cuenta durante {@code lockDuration}
     * y reinicia el contador para que, al expirar el bloqueo, haya de nuevo {@code maxAttempts} intentos.
     *
     * @return true si este intento provoco el bloqueo.
     */
    public boolean registerFailedLogin(int maxAttempts, Duration lockDuration, Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;
            return true;
        }
        return false;
    }

    public void registerSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void unlock() {
        registerSuccessfulLogin();
    }

    // ---- Roles y jurisdiccion (RNF-12, US-26) ----

    public void changeRole(Role newRole, String newJurisdiction) {
        String normalized = newRole == Role.INSTITUCIONAL ? newJurisdiction : null;
        validateJurisdiction(newRole, normalized);
        this.role = newRole;
        this.jurisdiction = normalized;
    }

    private static void validateJurisdiction(Role role, String jurisdiction) {
        boolean hasJurisdiction = jurisdiction != null && !jurisdiction.isBlank();
        if (role == Role.INSTITUCIONAL && !hasJurisdiction) {
            throw new DomainRuleViolationException("Una cuenta institucional requiere una jurisdiccion");
        }
        if (role != Role.INSTITUCIONAL && jurisdiction != null) {
            throw new DomainRuleViolationException("Solo las cuentas institucionales tienen jurisdiccion");
        }
    }

    // ---- Perfil y zonas de interes (US-03) ----

    public void updateProfile(String displayName) {
        this.displayName = displayName;
    }

    public InterestZone addInterestZone(String label, double latitude, double longitude, int radiusMeters,
                                        int maxZones, Instant now) {
        if (interestZones.size() >= maxZones) {
            throw new DomainRuleViolationException(
                    "Se alcanzo el maximo de " + maxZones + " zonas de interes");
        }
        InterestZone zone = new InterestZone(this, label, latitude, longitude, radiusMeters, now);
        interestZones.add(zone);
        return zone;
    }

    public InterestZone removeInterestZone(UUID zoneId) {
        InterestZone zone = interestZones.stream()
                .filter(z -> z.getId().equals(zoneId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Zona de interes no encontrada: " + zoneId));
        interestZones.remove(zone);
        return zone;
    }
}
