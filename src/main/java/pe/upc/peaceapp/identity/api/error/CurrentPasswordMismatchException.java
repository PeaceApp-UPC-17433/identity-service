package pe.upc.peaceapp.identity.api.error;

public class CurrentPasswordMismatchException extends RuntimeException {
    public CurrentPasswordMismatchException() {
        super("La contrasena actual no es correcta");
    }
}
