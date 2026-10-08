package pe.upc.peaceapp.identity.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.identity.domain.event.IdentityEvent;
import pe.upc.peaceapp.identity.domain.event.OutboxEvent;
import pe.upc.peaceapp.identity.domain.repository.OutboxEventRepository;

import java.time.Clock;

/** Guarda los eventos de dominio en el outbox dentro de la transaccion que los origina. */
@Component
public class EventOutbox {

    private static final Logger log = LoggerFactory.getLogger(EventOutbox.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EventOutbox(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(IdentityEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new OutboxEvent(event.userId(), event.type(), payload, clock.instant()));
            log.info("Evento {} registrado en outbox para el usuario {}", event.type(), event.userId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + event.type(), e);
        }
    }
}
