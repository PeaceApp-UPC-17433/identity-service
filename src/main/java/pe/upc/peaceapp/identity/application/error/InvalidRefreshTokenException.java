package pe.upc.peaceapp.identity.application.error;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("La sesion es invalida o expiro");
    }
}
