package pe.upc.peaceapp.identity.api.dto;

public final class PasswordPolicy {
    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";
    public static final String MESSAGE = "La contrasena debe tener de 8 a 72 caracteres, con mayuscula, minuscula y numero";

    private PasswordPolicy() {
    }
}
