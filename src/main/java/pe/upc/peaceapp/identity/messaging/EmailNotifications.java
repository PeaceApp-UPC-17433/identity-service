package pe.upc.peaceapp.identity.messaging;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EmailNotifications {
    public static final String ROUTING_KEY = "notification.email-requested";

    private final IntegrationEventPublisher events;

    public EmailNotifications(IntegrationEventPublisher events) {
        this.events = events;
    }

    public void request(String template, String to, String locale, Map<String, String> params) {
        var event = new LinkedHashMap<String, Object>();
        event.put("eventId", UUID.randomUUID());
        event.put("occurredAt", Instant.now().toString());
        event.put("template", template);
        event.put("to", to);
        event.put("locale", locale);
        event.put("params", params);
        events.publish(ROUTING_KEY, event);
    }
}
