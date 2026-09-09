package pe.com.edu.prisma.common.exception;

/**
 * A request that's well-formed but violates a domain rule — e.g. "this
 * exam already has results, use the force-reload action" or "this DNI
 * already belongs to another student". {@code message} is user-facing,
 * same rule as {@link ResourceNotFoundException}.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String userFacingMessage) {
        super(userFacingMessage);
    }
}
