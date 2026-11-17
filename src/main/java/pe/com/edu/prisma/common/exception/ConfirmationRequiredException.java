package pe.com.edu.prisma.common.exception;

import java.util.Map;

/**
 * The request is valid but has a consequence the user must accept first
 * (e.g. closing a cycle early moves its end date to today). Answered as a
 * 409 with {@code requiresConfirmation: true}; the client asks the user and
 * repeats the request with {@code confirm: true}.
 */
public class ConfirmationRequiredException extends BusinessRuleException {

    // extra Problem Details properties the client needs to word the question (e.g. which fields are empty)
    private final Map<String, Object> properties;

    public ConfirmationRequiredException(String userFacingMessage) {
        this(userFacingMessage, Map.of());
    }

    public ConfirmationRequiredException(String userFacingMessage, Map<String, Object> properties) {
        super(userFacingMessage);
        this.properties = Map.copyOf(properties);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
