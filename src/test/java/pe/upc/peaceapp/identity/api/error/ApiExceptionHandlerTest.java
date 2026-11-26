package pe.upc.peaceapp.identity.api.error;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
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
