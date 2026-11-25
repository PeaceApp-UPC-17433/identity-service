package pe.upc.peaceapp.identity.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxIntegrationEventPublisher implements IntegrationEventPublisher {
    private final OutboxEventRepository repository;
    private final ObjectMapper json;

    public OutboxIntegrationEventPublisher(OutboxEventRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    /** MANDATORY: exige una transaccion activa; el evento nace atado al cambio de estado. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String routingKey, Object payload) {
        try {
            repository.save(new OutboxEvent(routingKey, json.writeValueAsString(payload)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + routingKey, e);
        }
    }
}
