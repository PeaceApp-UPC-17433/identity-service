package pe.upc.peaceapp.identity.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pe.upc.peaceapp.identity.IdentityServiceApplication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@CucumberContextConfiguration
@SpringBootTest(classes = IdentityServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationSteps {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate http;

    private final ObjectMapper mapper = new ObjectMapper();
    private ResponseEntity<String> response;

    @Dado("que no existe un usuario con el correo {string}")
    public void noExisteUsuario(String email) {
        // La base de Testcontainers parte vacia y cada escenario usa un correo propio.
    }

    @Dado("que existe un usuario registrado con el correo {string} y contraseña {string}")
    public void existeUsuario(String email, String password) {
        assertEquals(201, register(email, password).getStatusCode().value());
    }

    @Cuando("registro un usuario con los siguientes datos")
    public void registroConTabla(DataTable table) {
        Map<String, String> row = table.asMaps().get(0);
        response = register(row.get("email"), row.get("password"));
    }

    @Cuando("inicio sesión con el correo {string} y contraseña {string}")
    public void inicioSesion(String email, String password) {
        response = login(email, password);
    }

    @Cuando("intento iniciar sesión {int} veces con la contraseña {string} para {string}")
    public void intentosFallidos(int veces, String password, String email) {
        for (int i = 0; i < veces; i++) {
            login(email, password);
        }
    }

    @Entonces("el sistema responde con código {int}")
    public void respondeCodigo(int codigo) {
        assertEquals(codigo, response.getStatusCode().value());
    }

    @Y("la respuesta contiene un token de acceso")
    public void contieneToken() throws Exception {
        String token = mapper.readTree(response.getBody()).path("accessToken").asText();
        assertFalse(token.isBlank());
    }

    private ResponseEntity<String> register(String email, String password) {
        return http.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "password", password), String.class);
    }

    private ResponseEntity<String> login(String email, String password) {
        return http.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), String.class);
    }
}
