package pe.upc.peaceapp.identity.bdd.steps;

import io.cucumber.java.PendingException;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import io.jsonwebtoken.Claims;
import pe.upc.peaceapp.identity.bdd.ScenarioContext;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.security.JwtService;

import static org.assertj.core.api.Assertions.assertThat;

/** US-26: acceso institucional al panel. */
public class AccesoInstitucionalSteps {

    private static final String INSTITUTIONAL_PASSWORD = "Institucional2026";

    private final ScenarioContext ctx;
    private final JwtService jwtService;
    private String institutionalEmail;

    public AccesoInstitucionalSteps(ScenarioContext ctx, JwtService jwtService) {
        this.ctx = ctx;
        this.jwtService = jwtService;
    }

    @Dado("que el administrador ha iniciado sesión en PeaceApp")
    public void administradorConSesion() {
        ctx.createUser("admin@peaceapp.pe", "Admin2026", Role.ADMINISTRADOR, null, true);
        assertThat(ctx.login("admin@peaceapp.pe", "Admin2026").getStatus()).isEqualTo(200);
    }

    @Dado("que el usuario {string} con rol {string} ha iniciado sesión en PeaceApp")
    public void usuarioConRolYSesion(String email, String rol) {
        ctx.createUser(email, "Segura2026", Role.valueOf(rol), null, true);
        assertThat(ctx.login(email, "Segura2026").getStatus()).isEqualTo(200);
    }

    @Cuando("crea una cuenta institucional con el correo {string} para la jurisdicción {string}")
    public void creaCuentaInstitucional(String email, String jurisdiccion) {
        throw new PendingException("No existe endpoint de administracion para crear cuentas institucionales");
    }

    @Entonces("la cuenta queda creada con rol {string} y jurisdicción {string}")
    public void cuentaCreadaConJurisdiccion(String rol, String jurisdiccion) {
        throw new PendingException("No existe endpoint de administracion para crear cuentas institucionales");
    }

    @Y("la cuenta queda verificada sin requerir confirmación de correo")
    public void cuentaVerificadaSinConfirmacion() {
        throw new PendingException("No existe endpoint de administracion para crear cuentas institucionales");
    }

    @Dado("que existe una cuenta institucional {string} para la jurisdicción {string}")
    public void existeCuentaInstitucional(String email, String jurisdiccion) {
        institutionalEmail = email;
        ctx.createUser(email, INSTITUTIONAL_PASSWORD, Role.INSTITUCIONAL, jurisdiccion, true);
    }

    @Cuando("el representante inicia sesión con sus credenciales institucionales")
    public void representanteIniciaSesion() {
        ctx.login(institutionalEmail, INSTITUTIONAL_PASSWORD);
    }

    @Entonces("el token de acceso identifica el rol {string} y la jurisdicción {string}")
    public void tokenIdentificaRolYJurisdiccion(String rol, String jurisdiccion) {
        assertThat(ctx.lastResponse().getStatus()).isEqualTo(200);
        Claims claims = jwtService.parseClaims(ctx.accessToken());
        assertThat(claims.get("role", String.class)).isEqualTo(rol);
        assertThat(claims.get("jurisdiction", String.class)).isEqualTo(jurisdiccion);
    }

    @Cuando("intenta crear una cuenta institucional para la jurisdicción {string}")
    public void intentaCrearCuentaInstitucional(String jurisdiccion) {
        throw new PendingException("No existe endpoint de administracion para crear cuentas institucionales");
    }

    @Entonces("la operación es rechazada por falta de permisos")
    public void rechazadaPorPermisos() {
        assertThat(ctx.lastResponse().getStatus()).isEqualTo(403);
    }

    @Y("existe el usuario {string} con rol {string}")
    public void existeUsuarioConRol(String email, String rol) {
        ctx.createUser(email, "Segura2026", Role.valueOf(rol), null, true);
    }

    @Cuando("le asigna el rol {string} con la jurisdicción {string}")
    public void asignaRol(String rol, String jurisdiccion) {
        throw new PendingException("No existe endpoint de administracion para asignar roles");
    }

    @Entonces("la asignación resulta {string}")
    public void asignacionResulta(String resultado) {
        throw new PendingException("No existe endpoint de administracion para asignar roles");
    }
}
