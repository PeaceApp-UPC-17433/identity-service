package pe.upc.peaceapp.identity.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pe.upc.peaceapp.identity.IdentityServiceApplication;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpResponse<String> response;

    @Dado("que no existe un usuario con el correo {string}")
    public void noExisteUsuario(String email) {
        // La base de Testcontainers parte vacia y cada escenario usa un correo propio.
    }

    @Dado("que existe un usuario registrado con el correo {string} y contraseña {string}")
    public void existeUsuario(String email, String password) {
        assertEquals(201, post("/api/v1/auth/register", email, password).statusCode());
    }

    @Cuando("registro un usuario con los siguientes datos")
    public void registroConTabla(DataTable table) {
        Map<String, String> row = table.asMaps().get(0);
        response = post("/api/v1/auth/register", row.get("email"), row.get("password"));
    }

    @Cuando("inicio sesión con el correo {string} y contraseña {string}")
    public void inicioSesion(String email, String password) {
        response = post("/api/v1/auth/login", email, password);
    }

    @Cuando("intento iniciar sesión {int} veces con la contraseña {string} para {string}")
    public void intentosFallidos(int veces, String password, String email) {
        for (int i = 0; i < veces; i++) {
            post("/api/v1/auth/login", email, password);
        }
    }

    @Entonces("el sistema responde con código {int}")
    public void respondeCodigo(int codigo) {
        assertEquals(codigo, response.statusCode());
    }

    @Y("la respuesta contiene un token de acceso")
    public void contieneToken() throws Exception {
        String token = mapper.readTree(response.body()).path("accessToken").asText();
        assertFalse(token.isBlank());
    }

    private HttpResponse<String> post(String path, String email, String password) {
        try {
            String json = mapper.writeValueAsString(Map.of("email", email, "password", password));
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
