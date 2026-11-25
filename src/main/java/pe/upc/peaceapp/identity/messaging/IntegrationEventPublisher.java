package pe.upc.peaceapp.identity.messaging;

/**
 * Puerto de salida para eventos que cruzan el limite del servicio. La implementacion los guarda en el outbox dentro
 * de la transaccion actual; nunca se llama al broker desde una transaccion de base de datos.
 */
public interface IntegrationEventPublisher {
    void publish(String routingKey, Object payload);
}
