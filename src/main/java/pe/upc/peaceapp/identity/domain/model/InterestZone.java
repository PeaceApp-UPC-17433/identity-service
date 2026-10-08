package pe.upc.peaceapp.identity.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** US-03: zonas de interes del usuario, usadas para filtrar el resumen de incidentes. */
@Entity
@Table(name = "interest_zones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestZone {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    InterestZone(User user, String label, double latitude, double longitude, int radiusMeters, Instant now) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.createdAt = now;
    }
}
