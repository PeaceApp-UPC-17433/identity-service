package pe.upc.peaceapp.identity.bdd;

import pe.upc.peaceapp.identity.security.OpaqueTokens;
import java.time.Duration;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Estado compartido entre los pasos de un mismo escenario y helpers para invocar la API. */
@Component
@ScenarioScope
public class ScenarioContext {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private MockHttpServletResponse lastResponse;
    private String accessToken;

    public ScenarioContext(MockMvc mockMvc, ObjectMapper objectMapper,
                           UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public MockHttpServletResponse postJson(String url, Object body) {
        return perform(post(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }

    public MockHttpServletResponse getAuthenticated(String url) {
        return perform(get(url).header("Authorization", "Bearer " + accessToken));
    }

    public MockHttpServletResponse postJsonAuthenticated(String url, Object body) {
        return perform(post(url).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }

    public MockHttpServletResponse register(String email, String password) {
        return postJson("/api/v1/auth/register", Map.of("email", email, "password", password));
    }

    public MockHttpServletResponse login(String email, String password) {
        MockHttpServletResponse response = postJson("/api/v1/auth/login", Map.of("email", email, "password", password));
        if (response.getStatus() == 200) {
            accessToken = json(response).path("accessToken").asText(null);
        }
        return response;
    }

    /** Crea una cuenta directamente en la base, sin pasar por el flujo de registro. */
    public User createUser(String email, String password, Role role, String jurisdiction, boolean verified) {
        Instant now = Instant.now();
        String hash = passwordEncoder.encode(password);
        User user = verified
                ? User.createVerified(email, hash, role, jurisdiction, now)
                : User.registerCitizen(email, hash, OpaqueTokens.hash(OpaqueTokens.generate()), now.plus(Duration.ofDays(1)), now);
        return userRepository.save(user);
    }

    public JsonNode json(MockHttpServletResponse response) {
        try {
            String body = response.getContentAsString();
            return body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("La respuesta no es JSON valido", e);
        }
    }

    /** Texto completo de un error RFC 7807: detail + errores de validacion. */
    public String errorText(MockHttpServletResponse response) {
        JsonNode body = json(response);
        StringBuilder text = new StringBuilder(body.path("detail").asText(""));
        body.path("errors").forEach(e -> text.append(" | ").append(e.asText()));
        return text.toString();
    }

    private MockHttpServletResponse perform(MockHttpServletRequestBuilder request) {
        try {
            lastResponse = mockMvc.perform(request).andReturn().getResponse();
            lastResponse.setCharacterEncoding("UTF-8");
            return lastResponse;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public MockHttpServletResponse lastResponse() {
        return lastResponse;
    }

    public String accessToken() {
        return accessToken;
    }
}
