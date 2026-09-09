package pe.com.edu.prisma.common.exception;

/**
 * The {@code message} is shown to the end user as-is (via
 * {@link pe.com.edu.prisma.common.exception.GlobalExceptionHandler}), so
 * write it in plain Spanish a tutor/admin would understand — never a
 * technical fragment like an entity or field name.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String userFacingMessage) {
        super(userFacingMessage);
    }
}
