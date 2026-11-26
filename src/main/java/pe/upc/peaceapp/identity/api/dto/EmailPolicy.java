package pe.upc.peaceapp.identity.api.dto;

/**
 * Formato de correo que acepta la plataforma. @Email por sí solo da por válida la cadena vacía y direcciones como
 * "zj{@7533*3107}zj", y a esa dirección se manda el código de verificación. Este patrón exige una parte local
 * con los caracteres habituales y un dominio con al menos un punto.
 */
public final class EmailPolicy {
    public static final String REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    public static final String MESSAGE = "El correo no tiene un formato valido";

    private EmailPolicy() {
    }
}
