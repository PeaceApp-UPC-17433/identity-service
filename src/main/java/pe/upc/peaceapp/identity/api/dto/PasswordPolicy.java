package pe.upc.peaceapp.identity.api.dto;

final class PasswordPolicy {
    static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    static final String MESSAGE = "La contrasena debe contener al menos una letra y un numero";

    private PasswordPolicy() {
    }
}
