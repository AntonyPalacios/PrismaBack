package pe.com.edu.prisma.common;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

/**
 * Alphabetical order the way people in the academy expect it: case and
 * accents don't matter ("Álgebra" sorts with "Algebra", before "Estadística").
 * Every listing is ordered by the backend with this, never by the client.
 */
public final class SpanishOrder {

    private static final Locale SPANISH = Locale.forLanguageTag("es-PE");

    private SpanishOrder() {
    }

    /** Compares by the given text (A–Z, case/accent-insensitive), nulls last. */
    public static <T> Comparator<T> by(Function<T, String> key) {
        Collator collator = Collator.getInstance(SPANISH);
        collator.setStrength(Collator.PRIMARY);
        return Comparator.comparing(key, Comparator.nullsLast(collator::compare));
    }
}
