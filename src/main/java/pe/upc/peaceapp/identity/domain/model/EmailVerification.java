package pe.upc.peaceapp.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification {

    @Id
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant lastSentAt;

    @Column(nullable = false)
    private Instant createdAt;

    @jakarta.persistence.Version
    private Long version;

    public EmailVerification(UUID userId, String codeHash, Instant expiresAt, Instant now) {
        this.userId = userId;
        renew(codeHash, expiresAt, now);
        this.createdAt = now;
    }

    public void renew(String codeHash, Instant expiresAt, Instant now) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.lastSentAt = now;
    }
}
