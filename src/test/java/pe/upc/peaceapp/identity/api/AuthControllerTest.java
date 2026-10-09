
package pe.upc.peaceapp.identity.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.upc.peaceapp.identity.api.dto.LoginResponse;
import pe.upc.peaceapp.identity.application.AuthService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .build();
    }

    @Test
    void registerReturns201WhenDataIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "nuevo@peaceapp.pe",
                      "password": "clave12345"
                    }
                    """))
                .andExpect(status().isCreated());

        verify(authService).register(
                "nuevo@peaceapp.pe", "clave12345");
    }

    @Test
    void loginReturns200WithJwtToken() throws Exception {
        when(authService.login(
                "usuario@peaceapp.pe", "clave12345"))
                .thenReturn(new LoginResponse("jwt-ejemplo"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "usuario@peaceapp.pe",
                      "password": "clave12345"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
        .value("jwt-ejemplo"))
.andExpect(jsonPath("$.tokenType")
        .value("Bearer"));

        verify(authService).login(
                "usuario@peaceapp.pe", "clave12345");
    }

    @Test
    void registerRejectsMissingRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void loginRejectsMissingRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
