package pe.upc.peaceapp.identity.api.error;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void everyHandlerReturnsAProblemDetailWithAnErrorStatus() throws Exception {
        var handlers = Arrays.stream(ApiExceptionHandler.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ExceptionHandler.class)).toList();
        assertThat(handlers).isNotEmpty();
        for (Method method : handlers) {
            Object exception = method.getParameterTypes()[0] == MethodArgumentNotValidException.class
                    ? validationException()
                    : Mockito.mock(method.getParameterTypes()[0]);
            var problem = (ProblemDetail) method.invoke(handler, exception);
            assertThat(problem.getStatus()).as(method.getName()).isGreaterThanOrEqualTo(400);
        }
    }

    // Hallazgo de ZAP (A10:2025): un NUL (%00) en el correo llegaba a PostgreSQL, que lo rechaza con SQLSTATE 22021,
    // y la excepción salía como 500 con el SQL en el log. Es una entrada inválida del cliente: 400 sin detalles internos.
    @Test
    void aNulCharacterRejectedByTheDatabaseIsABadRequest() {
        var ex = new DataIntegrityViolationException("JDBC exception executing SQL [select ... where u1_0.email=?]",
                new SQLException("ERROR: invalid byte sequence for encoding \"UTF8\": 0x00", "22021"));

        var problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).doesNotContain("select", "UTF8", "0x00");
    }

    @Test
    void anyOtherIntegrityErrorStaysAServerErrorWithoutInternalDetails() {
        var ex = new DataIntegrityViolationException("could not execute statement [insert into users ...]",
                new SQLException("duplicate key value violates unique constraint", "23505"));

        var problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).doesNotContain("insert", "duplicate");
    }

    private static MethodArgumentNotValidException validationException() throws NoSuchMethodException {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "email", "obligatorio"));
        var parameter = new MethodParameter(ApiExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
        return new MethodArgumentNotValidException(parameter, binding);
    }

    @SuppressWarnings("unused")
    void dummy(String value) {
    }
}
