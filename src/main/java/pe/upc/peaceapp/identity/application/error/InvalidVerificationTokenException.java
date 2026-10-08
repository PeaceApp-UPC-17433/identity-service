package pe.upc.peaceapp.identity.application.error;

public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException() {
        super("El enlace de verificacion es invalido o expiro");
    }
}
