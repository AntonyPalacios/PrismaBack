package pe.com.edu.prisma.common.logging;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.StringJoiner;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Wraps every Prisma {@code @RestController} method to remember which
 * endpoint is running and with which (sanitized) arguments.
 *
 * It never logs errors: an exception just passes through, and the
 * {@code GlobalExceptionHandler} logs it once, adding the {@link EndpointCall}
 * left here. On success it only writes a DEBUG line with the duration.
 */
@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    @Around("within(pe.com.edu.prisma..*) && within(@org.springframework.web.bind.annotation.RestController *)")
    public Object aroundEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
        EndpointCall call = describe(joinPoint);
        if (call != null && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            call.storeIn(attrs.getRequest());
        }
        long start = System.nanoTime();
        Object result = joinPoint.proceed();
        if (call != null && log.isDebugEnabled()) {
            log.debug("{} took {} ms", call, (System.nanoTime() - start) / 1_000_000);
        }
        return result;
    }

    private static EndpointCall describe(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String endpoint = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();

            String[] names = signature.getParameterNames();
            Annotation[][] annotations = signature.getMethod().getParameterAnnotations();
            Object[] values = joinPoint.getArgs();

            StringJoiner arguments = new StringJoiner(", ", "{", "}");
            for (int i = 0; i < values.length; i++) {
                if (isInfrastructure(values[i])) {
                    continue;
                }
                String name = names != null && i < names.length ? names[i] : "arg" + i;
                arguments.add(name + "=" + LogSanitizer.sanitize(values[i], sensitiveOf(annotations[i])));
            }
            return new EndpointCall(endpoint, arguments.toString());
        } catch (RuntimeException e) {
            return null; // logging must never break the request
        }
    }

    // request/response objects, the logged-in user and validation results are not "the data that was sent"
    private static boolean isInfrastructure(Object value) {
        return value instanceof ServletRequest || value instanceof ServletResponse
                || value instanceof Principal || value instanceof UserDetails || value instanceof Errors;
    }

    private static Sensitive sensitiveOf(Annotation[] parameterAnnotations) {
        for (Annotation annotation : parameterAnnotations) {
            if (annotation instanceof Sensitive sensitive) {
                return sensitive;
            }
        }
        return null;
    }
}
