package pe.upc.peaceapp.identity.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** CA-07: formato estandar de error (RFC 7807 Problem Details) en todas las APIs. */
@RestControllerAdvice
public class ApiExceptionHandler {

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

    @ExceptionHandler(MfaExceptions.InvalidChallengeException.class)
    public ProblemDetail handleInvalidChallenge(MfaExceptions.InvalidChallengeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setProperty("code", "MFA_CHALLENGE_EXPIRED");
        return problem;
    }
}
