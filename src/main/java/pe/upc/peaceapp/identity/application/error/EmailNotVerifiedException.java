package pe.upc.peaceapp.identity.application.error;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Debes verificar tu correo antes de iniciar sesion");
    }
}
