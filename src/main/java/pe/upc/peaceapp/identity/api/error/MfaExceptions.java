package pe.upc.peaceapp.identity.api.error;

public final class MfaExceptions {
    private MfaExceptions() {
    }

    public static class InvalidMfaCodeException extends RuntimeException {
        public InvalidMfaCodeException() {
            super("El codigo es incorrecto");
        }
    }

    public static class MfaLockedException extends RuntimeException {
        private final long retryAfterSeconds;

        public MfaLockedException(long retryAfterSeconds) {
            super("Demasiados intentos. Intenta de nuevo en unos minutos");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public static class MfaStateException extends RuntimeException {
        private final String code;

        public MfaStateException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    public static class InvalidChallengeException extends RuntimeException {
        public InvalidChallengeException() {
            super("La sesion de verificacion vencio. Inicia sesion de nuevo");
        }
    }
}
