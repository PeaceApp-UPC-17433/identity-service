package pe.upc.peaceapp.identity.application.error;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("El correo ya se encuentra registrado");
    }
}
