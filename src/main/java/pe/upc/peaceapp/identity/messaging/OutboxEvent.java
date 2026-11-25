package pe.upc.peaceapp.identity.messaging;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Transactional outbox: el evento se escribe en la MISMA transaccion que el cambio de estado. */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() { }

    public OutboxEvent(String routingKey, String payload) {
        this.id = UUID.randomUUID();
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
}
