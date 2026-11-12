package pe.com.edu.prisma.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * What the {@link ControllerLoggingAspect} knows about the request being
 * served: the controller method and its arguments, already safe to log. It
 * travels in a request attribute so the {@code GlobalExceptionHandler} (the
 * only place that logs errors) can add it to its single log line.
 */
public record EndpointCall(String endpoint, String arguments) {

    private static final String ATTRIBUTE = EndpointCall.class.getName();

    public void storeIn(HttpServletRequest request) {
        request.setAttribute(ATTRIBUTE, this);
    }

    public static Optional<EndpointCall> from(HttpServletRequest request) {
        return request.getAttribute(ATTRIBUTE) instanceof EndpointCall call ? Optional.of(call) : Optional.empty();
    }

    /** e.g. {@code UserController#update args={id=5, request=UpdateUserRequest{...}}} */
    @Override
    public String toString() {
        return endpoint + " args=" + arguments;
    }
}
