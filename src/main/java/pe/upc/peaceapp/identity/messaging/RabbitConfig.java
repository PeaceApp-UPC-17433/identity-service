package pe.upc.peaceapp.identity.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange eventsExchange(@Value("${identity.messaging.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }
}
