package pe.com.edu.prisma.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.com.edu.prisma.common.logging.EndpointCall;
import pe.com.edu.prisma.common.logging.TraceIds;

/**
 * Every response body here is RFC 7807 (ProblemDetail). The {@code detail}
 * field is what a tutor/admin — not a developer — reads, so it is always
 * plain language, never an exception message or a stack trace.
 *
 * Every response also carries an {@code errorId}: the request's traceId,
 * which is printed on every log line of that request. If the end user
 * reports "me salió un error", ask for that id and search the log for it.
 *
 * This is the only place that logs errors, once per request (agreement 4.2
 * "Logs"): 500 at ERROR with the stack trace, 403/409 at WARN, and the
 * everyday 400/404/405 at INFO so they don't bury what matters.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_DETAIL =
            "Ocurrió un problema inesperado. Si el error persiste, comparte el código de error con soporte.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.info("404 on {}: {}", describe(request), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("409 on {}: {}", describe(request), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Valid, but the user must accept a consequence first: same 409, plus a
    // flag so the client asks and repeats the request with confirm=true.
    @ExceptionHandler(ConfirmationRequiredException.class)
    public ProblemDetail handleConfirmationRequired(ConfirmationRequiredException ex, HttpServletRequest request) {
        log.info("409 on {}: confirmation required", describe(request));
        ProblemDetail problem = problem(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("requiresConfirmation", true);
        return problem;
    }

    // A body that can't be read at all (broken JSON, a date like "abc", an
    // unknown status): a 400, never the generic 500 below.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // no exception message: it quotes the body that was sent
        log.info("400 on {}: unreadable request body", describe(request));
        return problem(HttpStatus.BAD_REQUEST, "No pudimos leer los datos enviados. Revisa el formato e inténtalo de nuevo.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("403 on {}: {}", describe(request), ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // only the field names: the exception message carries the rejected values (personal data)
        log.info("400 on {}: invalid fields {}", describe(request), invalidFieldNames(ex));
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        if (detail.isBlank()) {
            detail = "Revisa los datos ingresados: algunos campos no son válidos.";
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, detail);
        problem.setProperty("fields", fieldErrors(ex));
        return problem;
    }

    // e.g. a POST to /api/v1/areas: areas and courses are read-only data, so
    // their write endpoints don't exist. A 405, never the generic 500 below.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        log.info("405 on {}", describe(request));
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Esta operación no está disponible.");
    }

    // A URL that maps to nothing (e.g. the removed PUT /exam-types/{id}/courses):
    // a plain 404, never the generic 500 below.
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        log.info("404 on {}: no such endpoint", describe(request));
        return problem(HttpStatus.NOT_FOUND, "No encontramos lo que buscas.");
    }

    // Catch-all: anything not handled above is a bug, not an expected
    // outcome — the user only ever sees GENERIC_DETAIL + the errorId;
    // the real exception (with stack trace) goes to the log at ERROR.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("500 on {}: unhandled exception", describe(request), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_DETAIL);
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorId", TraceIds.current());
        return problem;
    }

    /** "DELETE /api/v1/users/5 (UserController#deactivate args={id=5})"; no endpoint part if the request never reached a controller. */
    private static String describe(HttpServletRequest request) {
        String route = request.getMethod() + " " + request.getRequestURI();
        return EndpointCall.from(request).map(call -> route + " (" + call + ")").orElse(route);
    }

    private static List<String> invalidFieldNames(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).distinct().toList();
    }

    private List<String> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
    }
}
