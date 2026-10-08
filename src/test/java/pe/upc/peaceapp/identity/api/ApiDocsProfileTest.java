package pe.upc.peaceapp.identity.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La documentacion (OpenAPI + Scalar) existe en dev y no se expone en prod. "test" va al final para usar H2. */
class ApiDocsProfileTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"dev", "test"})
    class Dev {

        @Autowired
        private MockMvc mvc;

        @Test
        void exponeContratoOpenApiYScalar() throws Exception {
            mvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("PeaceApp - identity-service"))
                    .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                    .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                    // Endpoints publicos sin Bearer; protegidos con Bearer; sin rutas internas de Scalar.
                    .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.security").doesNotExist())
                    .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security[0].bearerAuth").exists())
                    .andExpect(jsonPath("$.paths['/scalar']").doesNotExist());

            mvc.perform(get("/scalar"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("/v3/api-docs")));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"prod", "test"})
    class Prod {

        @Autowired
        private MockMvc mvc;

        @Test
        void noExponeDocumentacion() throws Exception {
            mvc.perform(get("/v3/api-docs")).andExpect(status().is(not(200)));
            mvc.perform(get("/scalar")).andExpect(status().is(not(200)));
        }
    }
}
