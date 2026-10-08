package pe.upc.peaceapp.identity.bdd.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.PendingException;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import pe.upc.peaceapp.identity.bdd.ScenarioContext;
import pe.upc.peaceapp.identity.domain.model.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** US-03: gestion de perfil y zonas de interes. */
public class GestionPerfilSteps {

    private static final String ZONES_URL = "/api/v1/users/me/interest-zones";

    private final ScenarioContext ctx;

    public GestionPerfilSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @Dado("que el usuario {string} ha iniciado sesión en PeaceApp")
    public void usuarioConSesion(String email) {
        ctx.createUser(email, "Segura2026", Role.CIUDADANO, null, true);
        assertThat(ctx.login(email, "Segura2026").getStatus()).isEqualTo(200);
    }

    // Regex en lugar de {double}: con "# language: es" Cucumber parsea los decimales con coma.
    @Cuando("^agrega la zona de interés \"([^\"]*)\" en latitud (-?[\\d.]+), longitud (-?[\\d.]+) con radio de (\\d+) metros$")
    public void agregaZona(String nombre, String latitud, String longitud, String radio) {
        agregarZona(nombre, latitud, longitud, radio);
    }

    @Entonces("su perfil muestra la zona de interés {string}")
    public void perfilMuestraZona(String nombre) {
        assertThat(nombresDeZonas()).contains(nombre);
    }

    @Y("se notifica a los demás servicios la actualización de sus zonas de interés")
    public void seNotificaZonas() {
        throw new PendingException("No existe publicador de eventos de dominio (InterestZoneUpdated)");
    }

    @Entonces("el registro de la zona resulta {string}")
    public void registroZonaResulta(String resultado) {
        int status = ctx.lastResponse().getStatus();
        switch (resultado) {
            case "aceptado" -> assertThat(status).as("status HTTP").isEqualTo(201);
            case "rechazado" -> assertThat(status).as("status HTTP").isEqualTo(400);
            default -> throw new IllegalArgumentException("Resultado desconocido: " + resultado);
        }
    }

    @Dado("que el usuario ya tiene {int} zonas de interés registradas")
    public void yaTieneZonas(int cantidad) {
        for (int i = 1; i <= cantidad; i++) {
            assertThat(agregarZona("Zona " + i, "-12.1", "-77.0", "300").getStatus()).isEqualTo(201);
        }
    }

    @Entonces("la zona es rechazada con el mensaje {string}")
    public void zonaRechazada(String mensaje) {
        assertThat(ctx.lastResponse().getStatus()).as("status HTTP").isBetween(400, 499);
        assertThat(ctx.errorText(ctx.lastResponse())).contains(mensaje);
    }

    @Dado("que el usuario tiene registradas las siguientes zonas de interés:")
    public void tieneZonas(DataTable tabla) {
        for (Map<String, String> fila : tabla.asMaps()) {
            assertThat(agregarZona(fila.get("nombre"), fila.get("latitud"), fila.get("longitud"), fila.get("radio"))
                    .getStatus()).isEqualTo(201);
        }
    }

    @Cuando("elimina la zona de interés {string}")
    public void eliminaZona(String nombre) {
        throw new PendingException("No existe endpoint para eliminar zonas de interes");
    }

    @Entonces("su perfil muestra únicamente la zona de interés {string}")
    public void perfilMuestraUnicamente(String nombre) {
        assertThat(nombresDeZonas()).containsExactly(nombre);
    }

    @Cuando("actualiza su nombre visible a {string}")
    public void actualizaNombreVisible(String nombre) {
        throw new PendingException("User no tiene nombre visible ni existe endpoint para actualizarlo");
    }

    @Entonces("su perfil muestra el nombre visible {string}")
    public void perfilMuestraNombre(String nombre) {
        assertThat(ctx.json(ctx.getAuthenticated("/api/v1/users/me")).path("displayName").asText()).isEqualTo(nombre);
    }

    private org.springframework.mock.web.MockHttpServletResponse agregarZona(String nombre, String latitud,
                                                                            String longitud, String radio) {
        return ctx.postJsonAuthenticated(ZONES_URL, Map.of(
                "label", nombre,
                "latitude", Double.parseDouble(latitud),
                "longitude", Double.parseDouble(longitud),
                "radiusMeters", Integer.parseInt(radio)));
    }

    private List<String> nombresDeZonas() {
        var perfil = ctx.json(ctx.getAuthenticated("/api/v1/users/me"));
        List<String> nombres = new ArrayList<>();
        perfil.path("interestZones").forEach(z -> nombres.add(z.path("label").asText()));
        return nombres;
    }
}
