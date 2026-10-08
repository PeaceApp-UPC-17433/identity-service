package pe.upc.peaceapp.identity.application;

import java.util.Locale;

/** Los correos se comparan sin distinguir mayusculas ni espacios: "A@X.pe " y "a@x.pe" son la misma cuenta. */
final class EmailAddress {

    private EmailAddress() {
    }

    static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
