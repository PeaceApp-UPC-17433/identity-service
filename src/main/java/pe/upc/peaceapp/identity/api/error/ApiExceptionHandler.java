package pe.upc.peaceapp.identity.api.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/** CA-07: formato estandar de error (RFC 7807 Problem Details) en todas las APIs. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    /** SQLSTATE character_not_in_repertoire: PostgreSQL no admite el caracter NUL en un texto. */
    private static final String INVALID_CHARACTER = "22021";

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o mas campos no son validos");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ProblemDetail handleInvalidResetToken(InvalidResetTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CurrentPasswordMismatchException.class)
    public ProblemDetail handleCurrentPasswordMismatch(CurrentPasswordMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ProblemDetail handleNotFound(java.util.NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(VerificationExceptions.InvalidCodeException.class)
    public ProblemDetail handleInvalidCode(VerificationExceptions.InvalidCodeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("code", "INVALID_CODE");
        return problem;
    }

    @ExceptionHandler(VerificationExceptions.TooManyAttemptsException.class)
    public ProblemDetail handleTooManyAttempts(VerificationExceptions.TooManyAttemptsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setProperty("code", "TOO_MANY_ATTEMPTS");
        return problem;
    }

    @ExceptionHandler(VerificationExceptions.ResendTooSoonException.class)
    public ProblemDetail handleResendTooSoon(VerificationExceptions.ResendTooSoonException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setProperty("code", "RESEND_TOO_SOON");
        problem.setProperty("retryAfterSeconds", ex.retryAfterSeconds());
        return problem;
    }

    @ExceptionHandler(VerificationExceptions.AlreadyVerifiedException.class)
    public ProblemDetail handleAlreadyVerified(VerificationExceptions.AlreadyVerifiedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("code", "ALREADY_VERIFIED");
        return problem;
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentUpdate(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "La informacion cambio mientras se procesaba. Intenta de nuevo.");
    }

    @ExceptionHandler(MfaExceptions.InvalidMfaCodeException.class)
    public ProblemDetail handleInvalidMfaCode(MfaExceptions.InvalidMfaCodeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("code", "INVALID_MFA_CODE");
        return problem;
    }

    @ExceptionHandler(MfaExceptions.MfaLockedException.class)
    public ProblemDetail handleMfaLocked(MfaExceptions.MfaLockedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setProperty("code", "MFA_LOCKED");
        problem.setProperty("retryAfterSeconds", ex.retryAfterSeconds());
        return problem;
    }

    @ExceptionHandler(MfaExceptions.MfaStateException.class)
    public ProblemDetail handleMfaState(MfaExceptions.MfaStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("code", ex.code());
        return problem;
    }

    /**
     * PostgreSQL rechaza el caracter NUL en un texto (SQLSTATE 22021). Llega desde el cliente, por ejemplo un
     * "%00" en el correo, así que es una petición inválida. Cualquier otro error de integridad sigue siendo un
     * 500, pero sin exponer el SQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        if (INVALID_CHARACTER.equals(sqlState(ex))) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El texto contiene caracteres no permitidos");
        }
        log.error("Error de integridad de datos", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operacion");
    }

    private static String sqlState(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) return sql.getSQLState();
        }
        return null;
    }

    @ExceptionHandler(MfaExceptions.InvalidChallengeException.class)
    public ProblemDetail handleInvalidChallenge(MfaExceptions.InvalidChallengeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setProperty("code", "MFA_CHALLENGE_EXPIRED");
        return problem;
    }
}
