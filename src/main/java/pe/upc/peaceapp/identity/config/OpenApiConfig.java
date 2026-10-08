package pe.upc.peaceapp.identity.config;

import com.scalar.maven.webmvc.ScalarWebMvcAutoConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Documentacion de la API, solo en el perfil dev: contrato OpenAPI en /v3/api-docs y Scalar en /scalar,
 * con autenticacion Bearer (declarada en cada controlador protegido).
 * Scalar se importa aqui de forma explicita para que no pueda activarse fuera de dev.
 */
@Configuration
@Profile("dev")
@Import(ScalarWebMvcAutoConfiguration.class)
public class OpenApiConfig {

    @Bean
    public OpenAPI identityOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PeaceApp - identity-service")
                        .description("BC-01 Identity & Access: US-01, US-02, US-03, US-26")
                        .version("v1"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
