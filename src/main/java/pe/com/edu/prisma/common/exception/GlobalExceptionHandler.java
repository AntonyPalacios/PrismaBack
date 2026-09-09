package pe.com.edu.prisma.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Every response body here is RFC 7807 (ProblemDetail). The {@code detail}
 * field is what a tutor/admin — not a developer — reads, so it is always
 * plain language, never an exception message or a stack trace.
 *
 * Every response also carries an {@code errorId}: a UUID logged alongside
 * the real exception. If the end user reports "me salió un error", ask for
 * that id and grep the logs for it instead of trying to reproduce blind.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_DETAIL =
            "Ocurrió un problema inesperado. Si el error persiste, comparte el código de error con soporte.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String errorId = logAndTag(ex, request, HttpStatus.NOT_FOUND);
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), errorId);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        String errorId = logAndTag(ex, request, HttpStatus.CONFLICT);
        return problem(HttpStatus.CONFLICT, ex.getMessage(), errorId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String errorId = logAndTag(ex, request, HttpStatus.FORBIDDEN);
        return problem(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.", errorId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorId = logAndTag(ex, request, HttpStatus.BAD_REQUEST);
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        if (detail.isBlank()) {
            detail = "Revisa los datos ingresados: algunos campos no son válidos.";
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, detail, errorId);
        problem.setProperty("fields", fieldErrors(ex));
        return problem;
    }

    // Catch-all: anything not handled above is a bug, not an expected
    // outcome — the user only ever sees GENERIC_DETAIL + the errorId;
    // the real exception (with stack trace) goes to the log at ERROR.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Unhandled exception on {} {}", errorId, request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_DETAIL, errorId);
    }

    private ProblemDetail problem(HttpStatus status, String detail, String errorId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorId", errorId);
        return problem;
    }

    private String logAndTag(Exception ex, HttpServletRequest request, HttpStatus status) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] {} on {} {}: {}", errorId, status, request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        return errorId;
    }

    private List<String> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
    }
}
