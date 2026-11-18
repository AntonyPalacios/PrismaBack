package pe.com.edu.prisma.imports.impl;

import java.util.Locale;

/**
 * How both imports read a DNI cell (agreements of 2026-11-18): an
 * identification has at least 7 digits. "61180218.0", " 6118 0218 " →
 * "61180218"; a number of 7 digits lost its leading zero to Excel and gets
 * it back ("6184164" → "06184164"); a number with fewer than 7 significant
 * digits ("0", "11", "00000011") is no DNI. Letters and numbers (a
 * foreigner's card) stay as they are, in upper case.
 */
final class ImportDni {

    private static final int MIN_DIGITS = 7;

    private ImportDni() {
    }

    static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.replaceAll("\\s", "").replaceAll("\\.0+$", "").toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        if (value.matches("[0-9]+")) {
            String significant = value.replaceFirst("^0+", "");
            if (significant.length() < MIN_DIGITS) {
                return null;
            }
            return significant.length() == MIN_DIGITS ? "0" + significant : value;
        }
        return value;
    }
}
