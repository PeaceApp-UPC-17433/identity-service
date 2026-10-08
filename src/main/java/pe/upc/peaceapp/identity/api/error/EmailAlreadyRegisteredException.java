package pe.upc.peaceapp.identity.api.error;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("El correo ya se encuentra registrado: " + email);
    }
}
