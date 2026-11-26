package pe.upc.peaceapp.identity.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CIUDADANO;

    /** US-26: jurisdiccion asignada a cuentas institucionales. Null para los demas roles. */
    private String jurisdiction;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false, length = 5)
    private String locale = "es";

    private boolean mfaEnabled = false;

    @Column(length = 200)
    private String mfaSecret;

    @Column(length = 200)
    private String mfaPendingSecret;

    private long mfaLastStep = 0;

    private int mfaFailedAttempts = 0;

    private Instant mfaLockedUntil;

    private Instant deletedAt;

    private boolean emailVerified = false;

    private int failedLoginAttempts = 0;

    private boolean locked = false;

    /** Cache local de la reputacion calculada por validation-service (evento ReputationChanged). */
    private int reputationScore = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterestZone> interestZones = new ArrayList<>();

    public boolean isStaff() {
        return role == Role.MODERADOR || role == Role.ADMINISTRADOR;
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
