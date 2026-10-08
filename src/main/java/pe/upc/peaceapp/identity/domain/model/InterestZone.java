package pe.upc.peaceapp.identity.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** US-03: zonas de interes del usuario, usadas para filtrar el resumen de incidentes. */
@Entity
@Table(name = "interest_zones")
@Getter
@Setter
@NoArgsConstructor
public class InterestZone {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private int radiusMeters;

    public InterestZone(User user, String label, double latitude, double longitude, int radiusMeters) {
        this.user = user;
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
    }
}
