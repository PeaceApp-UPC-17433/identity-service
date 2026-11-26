package pe.upc.peaceapp.identity.application;

import pe.upc.peaceapp.identity.messaging.IntegrationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CapturingPublisher implements IntegrationEventPublisher {
    record Published(String routingKey, Map<String, Object> payload) {
        String template() { return (String) payload.get("template"); }
        @SuppressWarnings("unchecked")
        Map<String, String> params() { return (Map<String, String>) payload.get("params"); }
    }

    final List<Published> events = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public void publish(String routingKey, Object payload) {
        events.add(new Published(routingKey, (Map<String, Object>) payload));
    }
}
