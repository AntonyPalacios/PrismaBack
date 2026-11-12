package pe.com.edu.prisma.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks personal data so it never reaches the log in clear text: the
 * {@link LogSanitizer} prints it through the given {@link LogMask}.
 * Put it on DTO record components and on controller method parameters.
 */
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    LogMask value();
}
