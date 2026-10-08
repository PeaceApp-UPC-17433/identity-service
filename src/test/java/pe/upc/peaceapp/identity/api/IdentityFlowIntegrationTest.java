package pe.upc.peaceapp.identity.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import pe.upc.peaceapp.identity.MutableClock;
import pe.upc.peaceapp.identity.domain.event.OutboxEvent;
import pe.upc.peaceapp.identity.domain.repository.OutboxEventRepository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Flujo completo de BC-01 contra la API HTTP real (H2 en modo PostgreSQL + migraciones Flyway). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityFlowIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String ADMIN_EMAIL = "admin@peaceapp.pe";
    private static final String ADMIN_PASSWORD = "Admin12345";

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MutableClock clock;
    @Autowired
    private OutboxEventRepository outbox;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    // ------------------------------------------------------------------ US-01

    @Nested
    class Registro {

        @Test
        void registraCuentaPendienteDeVerificacionYPublicaEvento() throws Exception {
            String email = uniqueEmail();

            JsonNode body = json(send("/api/v1/auth/register", Map.of("email", email, "password", PASSWORD))
                    .andExpect(status().isCreated()));

            assertThat(body.get("email").asText()).isEqualTo(email);
            assertThat(body.get("verificationToken").asText()).isNotBlank();
            UUID userId = UUID.fromString(body.get("userId").asText());
            assertThat(outbox.findByAggregateIdOrderByOccurredAtAsc(userId))
                    .extracting(OutboxEvent::getEventType).containsExactly("UserRegistered");
        }

        @Test
        void rechazaCorreoDuplicadoSinDistinguirMayusculas() throws Exception {
            String email = uniqueEmail();
            register(email);

            send("/api/v1/auth/register", Map.of("email", email.toUpperCase(), "password", PASSWORD))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("El correo ya se encuentra registrado"));
        }

        @Test
        void rechazaContrasenaQueNoCumpleLaPolitica() throws Exception {
            send("/api/v1/auth/register", Map.of("email", uniqueEmail(), "password", "solotexto"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value(startsWith("password")));
        }

        @Test
        void verificaElCorreoUnaSolaVez() throws Exception {
            String email = uniqueEmail();
            String token = register(email);

            send("/api/v1/auth/login", credentials(email, PASSWORD)).andExpect(status().isForbidden());

            send("/api/v1/auth/verify-email", Map.of("token", token)).andExpect(status().isNoContent());
            send("/api/v1/auth/verify-email", Map.of("token", token)).andExpect(status().isBadRequest());

            send("/api/v1/auth/login", credentials(email, PASSWORD)).andExpect(status().isOk());
        }

        @Test
        void tokenDeVerificacionExpiradoSePuedeReenviar() throws Exception {
            String email = uniqueEmail();
            String expired = register(email);
            clock.advance(Duration.ofHours(25));

            send("/api/v1/auth/verify-email", Map.of("token", expired)).andExpect(status().isBadRequest());

            String fresh = json(send("/api/v1/auth/resend-verification", Map.of("email", email))
                    .andExpect(status().isAccepted())).get("verificationToken").asText();
            send("/api/v1/auth/verify-email", Map.of("token", fresh)).andExpect(status().isNoContent());
        }

        @Test
        void reenvioNoRevelaSiElCorreoExiste() throws Exception {
            send("/api/v1/auth/resend-verification", Map.of("email", uniqueEmail()))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.verificationToken").doesNotExist());
        }
    }

    // ------------------------------------------------------------------ US-02 / RNF-11

    @Nested
    class InicioDeSesion {

        @Test
        void emiteAccessYRefreshToken() throws Exception {
            String email = registerAndVerify();

            send("/api/v1/auth/login", credentials(email.toUpperCase(), PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        void mensajeGenericoParaCorreoInexistenteYContrasenaIncorrecta() throws Exception {
            String email = registerAndVerify();

            String unknown = json(send("/api/v1/auth/login", credentials(uniqueEmail(), PASSWORD))
                    .andExpect(status().isUnauthorized())).get("detail").asText();
            String wrong = json(send("/api/v1/auth/login", credentials(email, "Incorrecta1"))
                    .andExpect(status().isUnauthorized())).get("detail").asText();

            assertThat(unknown).isEqualTo(wrong).isEqualTo("Credenciales invalidas");
        }

        @Test
        void bloqueaTrasCincoIntentosFallidosYDesbloqueaAlExpirar() throws Exception {
            String email = registerAndVerify();
            for (int i = 0; i < 5; i++) {
                send("/api/v1/auth/login", credentials(email, "Incorrecta1")).andExpect(status().isUnauthorized());
            }

            // Con la contrasena correcta sigue bloqueada, y la respuesta no revela el bloqueo.
            send("/api/v1/auth/login", credentials(email, PASSWORD))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Credenciales invalidas"));

            clock.advance(Duration.ofMinutes(16));
            send("/api/v1/auth/login", credentials(email, PASSWORD)).andExpect(status().isOk());
        }

        @Test
        void registraEventoAccountLocked() throws Exception {
            String email = registerAndVerify();
            UUID userId = userIdOf(login(email, PASSWORD));
            for (int i = 0; i < 5; i++) {
                send("/api/v1/auth/login", credentials(email, "Incorrecta1"));
            }

            assertThat(outbox.findByAggregateIdOrderByOccurredAtAsc(userId))
                    .extracting(OutboxEvent::getEventType).contains("AccountLocked");
        }

        @Test
        void refreshRotaElTokenYDetectaReuso() throws Exception {
            String email = registerAndVerify();
            String firstRefresh = login(email, PASSWORD).get("refreshToken").asText();

            String secondRefresh = json(send("/api/v1/auth/refresh", Map.of("refreshToken", firstRefresh))
                    .andExpect(status().isOk())).get("refreshToken").asText();
            assertThat(secondRefresh).isNotEqualTo(firstRefresh);

            // Reusar el token ya rotado revoca todas las sesiones, incluida la nueva.
            send("/api/v1/auth/refresh", Map.of("refreshToken", firstRefresh)).andExpect(status().isUnauthorized());
            send("/api/v1/auth/refresh", Map.of("refreshToken", secondRefresh)).andExpect(status().isUnauthorized());
        }

        @Test
        void logoutInvalidaElRefreshToken() throws Exception {
            String refresh = login(registerAndVerify(), PASSWORD).get("refreshToken").asText();

            send("/api/v1/auth/logout", Map.of("refreshToken", refresh)).andExpect(status().isNoContent());
            send("/api/v1/auth/refresh", Map.of("refreshToken", refresh)).andExpect(status().isUnauthorized());
        }

        @Test
        void accessTokenExpiradoResponde401() throws Exception {
            String token = accessToken(registerAndVerify());
            clock.advance(Duration.ofMinutes(61));

            mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------------ US-03

    @Nested
    class Perfil {

        @Test
        void sinTokenResponde401EnFormatoProblemDetails() throws Exception {
            mvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void consultaYEditaElPerfil() throws Exception {
            String email = registerAndVerify();
            String token = accessToken(email);

            authed(get("/api/v1/users/me"), token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.role").value("CIUDADANO"))
                    .andExpect(jsonPath("$.emailVerified").value(true))
                    .andExpect(jsonPath("$.interestZones").isEmpty());

            authed(patch("/api/v1/users/me", Map.of("displayName", "Luis")), token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Luis"));
        }

        @Test
        void gestionaZonasDeInteres() throws Exception {
            String email = registerAndVerify();
            String token = accessToken(email);

            String zoneId = json(authed(post("/api/v1/users/me/interest-zones", zone("Casa", -12.12, -77.03)), token)
                    .andExpect(status().isCreated())).get("id").asText();

            authed(get("/api/v1/users/me/interest-zones"), token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].label").value("Casa"));
            authed(get("/api/v1/users/me"), token)
                    .andExpect(jsonPath("$.interestZones[0].id").value(zoneId));

            authed(delete("/api/v1/users/me/interest-zones/" + zoneId), token).andExpect(status().isNoContent());
            authed(delete("/api/v1/users/me/interest-zones/" + zoneId), token).andExpect(status().isNotFound());

            UUID userId = userIdOf(login(email, PASSWORD));
            assertThat(outbox.findByAggregateIdOrderByOccurredAtAsc(userId))
                    .extracting(OutboxEvent::getEventType)
                    .containsSubsequence("InterestZoneUpdated", "InterestZoneUpdated");
        }

        @Test
        void validaCoordenadas() throws Exception {
            String token = accessToken(registerAndVerify());

            authed(post("/api/v1/users/me/interest-zones", zone("Mal", 999, -77)), token)
                    .andExpect(status().isBadRequest());
        }

        @Test
        void limitaElNumeroDeZonas() throws Exception {
            String token = accessToken(registerAndVerify());
            for (int i = 0; i < 10; i++) {
                authed(post("/api/v1/users/me/interest-zones", zone("Zona " + i, -12, -77)), token)
                        .andExpect(status().isCreated());
            }

            authed(post("/api/v1/users/me/interest-zones", zone("Zona 11", -12, -77)), token)
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void noPuedeBorrarZonasDeOtroUsuario() throws Exception {
            String ownerToken = accessToken(registerAndVerify());
            String zoneId = json(authed(post("/api/v1/users/me/interest-zones", zone("Casa", -12, -77)), ownerToken))
                    .get("id").asText();

            String otherToken = accessToken(registerAndVerify());
            authed(delete("/api/v1/users/me/interest-zones/" + zoneId), otherToken).andExpect(status().isNotFound());
        }
    }

    // ------------------------------------------------------------------ US-26 / RNF-12

    @Nested
    class AccesoInstitucional {

        @Test
        void ciudadanoNoAccedeAAdministracion() throws Exception {
            String token = accessToken(registerAndVerify());

            authed(post("/api/v1/admin/institutional-accounts", institutional(uniqueEmail(), "Miraflores")), token)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void adminCreaCuentaInstitucionalYSuTokenLlevaLaJurisdiccion() throws Exception {
            String adminToken = adminToken();
            String email = uniqueEmail();

            authed(post("/api/v1/admin/institutional-accounts", institutional(email, "Miraflores")), adminToken)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("INSTITUCIONAL"))
                    .andExpect(jsonPath("$.jurisdiction").value("Miraflores"));

            JsonNode claims = claimsOf(login(email, PASSWORD).get("accessToken").asText());
            assertThat(claims.get("role").asText()).isEqualTo("INSTITUCIONAL");
            assertThat(claims.get("jurisdiction").asText()).isEqualTo("Miraflores");
        }

        @Test
        void cuentaInstitucionalRequiereJurisdiccion() throws Exception {
            authed(post("/api/v1/admin/institutional-accounts",
                    Map.of("email", uniqueEmail(), "password", PASSWORD, "jurisdiction", " ")), adminToken())
                    .andExpect(status().isBadRequest());
        }

        @Test
        void adminCambiaRolYDesbloqueaCuentas() throws Exception {
            String adminToken = adminToken();
            String email = registerAndVerify();
            UUID userId = userIdOf(login(email, PASSWORD));

            authed(put("/api/v1/admin/users/" + userId + "/role", Map.of("role", "INSTITUCIONAL")), adminToken)
                    .andExpect(status().isUnprocessableEntity());
            authed(put("/api/v1/admin/users/" + userId + "/role",
                    Map.of("role", "INSTITUCIONAL", "jurisdiction", "San Isidro")), adminToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jurisdiction").value("San Isidro"));

            for (int i = 0; i < 5; i++) {
                send("/api/v1/auth/login", credentials(email, "Incorrecta1"));
            }
            send("/api/v1/auth/login", credentials(email, PASSWORD)).andExpect(status().isUnauthorized());

            authed(post("/api/v1/admin/users/" + userId + "/unlock", Map.of()), adminToken)
                    .andExpect(status().isNoContent());
            send("/api/v1/auth/login", credentials(email, PASSWORD)).andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------------------ helpers

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@peaceapp.pe";
    }

    private Map<String, Object> credentials(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    private Map<String, Object> zone(String label, double latitude, double longitude) {
        return Map.of("label", label, "latitude", latitude, "longitude", longitude, "radiusMeters", 500);
    }

    private Map<String, Object> institutional(String email, String jurisdiction) {
        return Map.of("email", email, "password", PASSWORD, "jurisdiction", jurisdiction);
    }

    /** Registra y devuelve el token de verificacion. */
    private String register(String email) throws Exception {
        return json(send("/api/v1/auth/register", credentials(email, PASSWORD)).andExpect(status().isCreated()))
                .get("verificationToken").asText();
    }

    private String registerAndVerify() throws Exception {
        String email = uniqueEmail();
        send("/api/v1/auth/verify-email", Map.of("token", register(email))).andExpect(status().isNoContent());
        return email;
    }

    private JsonNode login(String email, String password) throws Exception {
        return json(send("/api/v1/auth/login", credentials(email, password)).andExpect(status().isOk()));
    }

    private String accessToken(String email) throws Exception {
        return login(email, PASSWORD).get("accessToken").asText();
    }

    private String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").asText();
    }

    private UUID userIdOf(JsonNode loginResponse) throws Exception {
        return UUID.fromString(claimsOf(loginResponse.get("accessToken").asText()).get("sub").asText());
    }

    private JsonNode claimsOf(String jwt) throws Exception {
        String payload = jwt.split("\\.")[1];
        return objectMapper.readTree(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8));
    }

    /** Peticion POST sin autenticacion. */
    private ResultActions send(String url, Object body) throws Exception {
        return mvc.perform(post(url, body));
    }

    private MockHttpServletRequestBuilder post(String url, Object body) throws Exception {
        return withJson(MockMvcRequestBuilders.post(url), body);
    }

    private MockHttpServletRequestBuilder patch(String url, Object body) throws Exception {
        return withJson(MockMvcRequestBuilders.patch(url), body);
    }

    private MockHttpServletRequestBuilder put(String url, Object body) throws Exception {
        return withJson(MockMvcRequestBuilders.put(url), body);
    }

    private ResultActions authed(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder request, Object body)
            throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
