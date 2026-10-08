package pe.upc.peaceapp.identity.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upc.peaceapp.identity.domain.event.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByAggregateIdOrderByOccurredAtAsc(UUID aggregateId);
}
