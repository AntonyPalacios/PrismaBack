package pe.com.edu.prisma.common.exception;

/**
 * The request is valid but has a consequence the user must accept first
 * (e.g. closing a cycle early moves its end date to today). Answered as a
 * 409 with {@code requiresConfirmation: true}; the client asks the user and
 * repeats the request with {@code confirm: true}.
 */
public class ConfirmationRequiredException extends BusinessRuleException {

    public ConfirmationRequiredException(String userFacingMessage) {
        super(userFacingMessage);
    }
}
