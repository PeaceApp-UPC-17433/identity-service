package pe.upc.peaceapp.identity.bdd.steps;

import io.cucumber.java.PendingException;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import pe.upc.peaceapp.identity.bdd.ScenarioContext;
import pe.upc.peaceapp.identity.domain.model.Role;
import pe.upc.peaceapp.identity.domain.model.User;
import pe.upc.peaceapp.identity.domain.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/** US-01: registro de usuario. */
public class RegistroUsuarioSteps {

    private final ScenarioContext ctx;
    private final UserRepository userRepository;

    public RegistroUsuarioSteps(ScenarioContext ctx, UserRepository userRepository) {
        this.ctx = ctx;
        this.userRepository = userRepository;
    }

    @Dado("que el correo {string} no está registrado en PeaceApp")
    public void correoNoRegistrado(String email) {
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Dado("que el correo {string} ya está registrado en PeaceApp")
    public void correoYaRegistrado(String email) {
        assertThat(ctx.register(email, "Segura2026").getStatus()).isEqualTo(201);
    }

    @Cuando("el ciudadano se registra con el correo {string} y la contraseña {string}")
    public void seRegistra(String email, String password) {
        ctx.register(email, password);
    }

    @Entonces("la cuenta queda creada con rol {string} y pendiente de verificación")
    public void cuentaCreadaPendiente(String rol) {
        assertThat(ctx.lastResponse().getStatus()).isEqualTo(201);
        User user = userRepository.findAll().getFirst();
        assertThat(user.getRole()).isEqualTo(Role.valueOf(rol));
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Y("se envía un correo de verificación a {string}")
    public void seEnviaCorreoVerificacion(String email) {
        throw new PendingException("No existe un puerto de envio de correo (EmailSender) que se pueda verificar");
    }

    @Y("el sistema responde {string}")
    public void elSistemaResponde(String mensaje) {
        assertThat(ctx.lastResponse().getStatus()).isEqualTo(201);
        assertThat(ctx.json(ctx.lastResponse()).path("message").asText())
                .as("mensaje de la respuesta de registro")
                .isEqualTo(mensaje);
    }

    @Entonces("el registro es rechazado con el mensaje {string}")
    public void registroRechazadoConMensaje(String mensaje) {
        assertThat(ctx.lastResponse().getStatus()).as("status HTTP").isEqualTo(409);
        assertThat(ctx.errorText(ctx.lastResponse())).contains(mensaje);
    }

    @Y("existe una sola cuenta con el correo {string}")
    public void existeUnaSolaCuenta(String email) {
        long cuentas = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                .count();
        assertThat(cuentas).isEqualTo(1);
    }

    @Entonces("el registro es rechazado indicando {string}")
    public void registroRechazadoIndicando(String motivo) {
        assertThat(ctx.lastResponse().getStatus()).as("status HTTP").isEqualTo(400);
        assertThat(ctx.errorText(ctx.lastResponse())).contains(motivo);
    }

    @Y("no se crea ninguna cuenta con el correo {string}")
    public void noSeCreaCuenta(String email) {
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Dado("que el ciudadano se registró con el correo {string} hace {int} horas")
    public void seRegistroHaceHoras(String email, int horas) {
        throw new PendingException("No existe flujo de verificacion de correo (token + endpoint) ni Clock inyectable");
    }

    @Cuando("confirma su correo con el enlace de verificación recibido")
    public void confirmaCorreo() {
        throw new PendingException("No existe endpoint de verificacion de correo");
    }

    @Entonces("la verificación resulta {string}")
    public void verificacionResulta(String resultado) {
        throw new PendingException("No existe endpoint de verificacion de correo");
    }
}
