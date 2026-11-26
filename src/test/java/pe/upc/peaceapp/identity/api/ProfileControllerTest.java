
package pe.upc.peaceapp.identity.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.upc.peaceapp.identity.application.AccountService;
import pe.upc.peaceapp.identity.application.ProfileService;
import pe.upc.peaceapp.identity.domain.model.InterestZone;
import pe.upc.peaceapp.identity.domain.model.User;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileControllerTest {

    private ProfileService profileService;
    private AccountService accountService;
    private MockMvc mockMvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        accountService = mock(AccountService.class);
        userId = UUID.randomUUID();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProfileController(profileService, accountService))
                .build();
    }

    // Hallazgo de ZAP: @Email da por válida la cadena vacía y es permisivo, así que PUT /users/me dejaba la cuenta
    // sin correo (el usuario ya no podía iniciar sesión) y mandaba el código de verificación a "" o a "zj{@7533*3107}zj".
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "zaproxy@example.com'", "zj{@7533*3107}zj", "sin-arroba.pe", "a@b"})
    void updateProfileRejectsAnInvalidEmail(String email) throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void updateProfileAcceptsAValidEmail() throws Exception {
        User user = new User("nuevo+avisos@sub.peaceapp.pe", "hash");
        user.setId(userId);
        when(accountService.updateProfile(eq(userId), any())).thenReturn(user);

        mockMvc.perform(put("/api/v1/users/me")
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nuevo+avisos@sub.peaceapp.pe\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getProfileReturnsUserData() throws Exception {
        User user = new User("usuario@peaceapp.pe", "hash");
        user.setId(userId);

        when(profileService.getById(userId)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me")
                .principal(new TestingAuthenticationToken(
                        userId.toString(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("usuario@peaceapp.pe"))
                .andExpect(jsonPath("$.id")
                        .value(userId.toString()));

        verify(profileService).getById(userId);
    }

    @Test
    void addInterestZoneReturns201() throws Exception {
        User user = new User("usuario@peaceapp.pe", "hash");
        user.setId(userId);

        InterestZone zone = new InterestZone(
                user, "Mi barrio", -12.0464, -77.0428, 500
        );

        when(profileService.addInterestZone(
                eq(userId), eq("Mi barrio"),
                eq(-12.0464), eq(-77.0428), eq(500)))
                .thenReturn(zone);

        mockMvc.perform(post("/api/v1/users/me/interest-zones")
                .principal(new TestingAuthenticationToken(
                        userId.toString(), null))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "label": "Mi barrio",
                      "latitude": -12.0464,
                      "longitude": -77.0428,
                      "radiusMeters": 500
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label")
                        .value("Mi barrio"))
                .andExpect(jsonPath("$.radiusMeters")
                        .value(500));

        verify(profileService).addInterestZone(
                userId, "Mi barrio", -12.0464, -77.0428, 500);
    }

    @Test
    void addInterestZoneRejectsMissingBody() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/interest-zones")
                .principal(new TestingAuthenticationToken(
                        userId.toString(), null))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
    }
}
