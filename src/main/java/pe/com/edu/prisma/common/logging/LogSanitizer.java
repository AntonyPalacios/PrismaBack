package pe.com.edu.prisma.common.logging;

import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Turns a controller argument into text that is safe to log (agreement 4.2
 * "Logs"). It works as an allow-list: only what is listed here is printed.
 *
 * <ul>
 *   <li>ids, numbers, booleans, enums and dates: as they are;</li>
 *   <li>text: as it is (cut at {@value #MAX_TEXT} characters), unless it is
 *       marked {@link Sensitive}, then through its {@link LogMask};</li>
 *   <li>collections, maps and arrays: only their size;</li>
 *   <li>uploaded files: only name and size;</li>
 *   <li>records (our DTOs): field by field, with these same rules;</li>
 *   <li>anything else: only its type name.</li>
 * </ul>
 */
public final class LogSanitizer {

    static final int MAX_TEXT = 80;
    private static final int MAX_DEPTH = 3;

    private LogSanitizer() {
    }

    public static String sanitize(Object value) {
        return sanitize(value, null, 0);
    }

    public static String sanitize(Object value, Sensitive sensitive) {
        return sanitize(value, sensitive, 0);
    }

    private static String sanitize(Object value, Sensitive sensitive, int depth) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Optional<?> optional) {
            return sanitize(optional.orElse(null), sensitive, depth);
        }
        if (sensitive != null) {
            // whatever the type, a sensitive value is only ever printed masked
            return sensitive.value().apply(String.valueOf(value));
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>
                || value instanceof UUID || value instanceof TemporalAccessor || value instanceof Date) {
            return value.toString();
        }
        if (value instanceof CharSequence text) {
            return quote(text.toString());
        }
        if (value instanceof Collection<?> collection) {
            return "[size=" + collection.size() + "]";
        }
        if (value instanceof Map<?, ?> map) {
            return "{size=" + map.size() + "}";
        }
        if (value.getClass().isArray()) {
            return "[size=" + Array.getLength(value) + "]";
        }
        if (value instanceof MultipartFile file) {
            return "file(name=" + quote(file.getOriginalFilename()) + ", size=" + file.getSize() + ")";
        }
        if (value.getClass().isRecord() && depth < MAX_DEPTH) {
            return sanitizeRecord(value, depth);
        }
        return "<" + value.getClass().getSimpleName() + ">";
    }

    private static String sanitizeRecord(Object record, int depth) {
        StringJoiner fields = new StringJoiner(", ", record.getClass().getSimpleName() + "{", "}");
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            String text;
            try {
                Object fieldValue = component.getAccessor().invoke(record);
                text = sanitize(fieldValue, component.getAnnotation(Sensitive.class), depth + 1);
            } catch (ReflectiveOperationException | RuntimeException e) {
                text = "<unreadable>"; // logging must never break the request
            }
            fields.add(component.getName() + "=" + text);
        }
        return fields.toString();
    }

    private static String quote(String text) {
        if (text == null) {
            return "null";
        }
        String shown = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) + "..." : text;
        // one log line per event: no line breaks coming from user input
        return "\"" + shown.replaceAll("[\\r\\n]+", " ") + "\"";
    }
}
