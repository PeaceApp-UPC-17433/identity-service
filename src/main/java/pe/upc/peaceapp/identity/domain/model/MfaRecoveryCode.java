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
@Table(name = "mfa_recovery_codes")
@Getter
@Setter
@NoArgsConstructor
public class MfaRecoveryCode {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String codeHash;

    private Instant usedAt;

    @Column(nullable = false)
    private Instant createdAt;

    public MfaRecoveryCode(UUID userId, String codeHash, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.codeHash = codeHash;
        this.createdAt = now;
    }
}
