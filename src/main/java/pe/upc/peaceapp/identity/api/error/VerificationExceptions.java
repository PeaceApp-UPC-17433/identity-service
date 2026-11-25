package pe.upc.peaceapp.identity.api.error;

public final class VerificationExceptions {
    private VerificationExceptions() {
    }

    public static class InvalidCodeException extends RuntimeException {
        public InvalidCodeException() {
            super("El codigo es incorrecto o vencio");
        }
    }

    public static class TooManyAttemptsException extends RuntimeException {
        public TooManyAttemptsException() {
            super("Demasiados intentos. Solicita un codigo nuevo");
        }
    }

    public static class ResendTooSoonException extends RuntimeException {
        private final long retryAfterSeconds;

        public ResendTooSoonException(long retryAfterSeconds) {
            super("Espera " + retryAfterSeconds + " segundos para pedir otro codigo");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public static class AlreadyVerifiedException extends RuntimeException {
        public AlreadyVerifiedException() {
            super("El correo ya esta verificado");
        }
    }
}
