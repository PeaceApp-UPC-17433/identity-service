package pe.upc.peaceapp.identity.bdd.steps;

import io.cucumber.java.PendingException;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import pe.upc.peaceapp.identity.bdd.ScenarioContext;
import pe.upc.peaceapp.identity.domain.model.Role;

import static org.assertj.core.api.Assertions.assertThat;

/** US-02: inicio de sesion. */
public class InicioSesionSteps {

    private final ScenarioContext ctx;

    public InicioSesionSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @Dado("que existe una cuenta verificada con el correo {string} y la contraseña {string}")
    public void cuentaVerificada(String email, String password) {
        ctx.createUser(email, password, Role.CIUDADANO, null, true);
    }

    @Dado("que existe una cuenta sin verificar con el correo {string} y la contraseña {string}")
    public void cuentaSinVerificar(String email, String password) {
        ctx.createUser(email, password, Role.CIUDADANO, null, false);
    }

    @Cuando("el usuario inicia sesión con el correo {string} y la contraseña {string}")
    public void elUsuarioIniciaSesion(String email, String password) {
        ctx.login(email, password);
    }

    @Cuando("inicia sesión con el correo {string} y la contraseña {string}")
    public void iniciaSesion(String email, String password) {
        ctx.login(email, password);
    }

    @Entonces("el usuario queda autenticado")
    public void quedaAutenticado() {
        assertThat(ctx.lastResponse().getStatus()).isEqualTo(200);
    }

    @Y("recibe un token de acceso y un token de renovación de sesión")
    public void recibeTokens() {
        var body = ctx.json(ctx.lastResponse());
        assertThat(body.path("accessToken").asText()).as("accessToken").isNotBlank();
        assertThat(body.path("refreshToken").asText()).as("refreshToken").isNotBlank();
    }

    @Entonces("el acceso es rechazado con el mensaje {string}")
    public void accesoRechazado(String mensaje) {
        assertThat(ctx.lastResponse().getStatus()).as("status HTTP").isEqualTo(401);
        assertThat(ctx.errorText(ctx.lastResponse())).isEqualTo(mensaje);
    }

    @Dado("que el usuario falló {int} veces consecutivas al iniciar sesión")
    public void fallosConsecutivos(int intentos) {
        for (int i = 0; i < intentos; i++) {
            ctx.login("ana@peaceapp.pe", "Incorrecta99");
        }
    }

    @Entonces("el inicio de sesión resulta {string}")
    public void inicioSesionResulta(String resultado) {
        switch (resultado) {
            case "exitoso" -> assertThat(ctx.lastResponse().getStatus()).as("status HTTP").isEqualTo(200);
            case "bloqueado" -> assertThat(ctx.lastResponse().getStatus()).as("status HTTP").isNotEqualTo(200);
            default -> throw new IllegalArgumentException("Resultado desconocido: " + resultado);
        }
    }

    @Dado("que la cuenta {string} fue bloqueada por intentos fallidos")
    public void cuentaBloqueada(String email) {
        for (int i = 0; i < 5; i++) {
            ctx.login(email, "Incorrecta99");
        }
    }

    @Dado("que la cuenta {string} fue bloqueada hace {int} minutos")
    public void cuentaBloqueadaHace(String email, int minutos) {
        throw new PendingException("User no registra el instante del bloqueo (lockedAt/lockedUntil) ni hay Clock inyectable");
    }
}
