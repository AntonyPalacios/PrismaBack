package pe.com.edu.prisma.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * The id that ties an error response to its log lines. Micrometer Tracing
 * puts the request's {@code traceId} in the MDC, so the same value is printed
 * on every log line of the request and returned to the user as {@code errorId}.
 */
public final class TraceIds {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";

    private TraceIds() {
    }

    /** The current request's traceId; a random id only if there is no trace (not expected inside a request). */
    public static String current() {
        String traceId = MDC.get(TRACE_ID);
        return traceId != null && !traceId.isBlank() ? traceId : UUID.randomUUID().toString();
    }
}
