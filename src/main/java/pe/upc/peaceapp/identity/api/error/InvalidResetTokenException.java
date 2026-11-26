package pe.upc.peaceapp.identity.api.error;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("El enlace de recuperacion es invalido o ya expiro");
    }
}
