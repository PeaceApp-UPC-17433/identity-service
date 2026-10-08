package pe.upc.peaceapp.identity.api.error;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("La cuenta esta bloqueada por intentos fallidos consecutivos");
    }
}
