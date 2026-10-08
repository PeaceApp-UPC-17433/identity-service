package pe.upc.peaceapp.identity.domain.exception;

/** Una operacion viola una regla del dominio (p. ej. limite de zonas de interes). */
public class DomainRuleViolationException extends RuntimeException {
    public DomainRuleViolationException(String message) {
        super(message);
    }
}
