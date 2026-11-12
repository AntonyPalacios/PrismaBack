package pe.com.edu.prisma.common.logging;

/**
 * How a piece of personal data is printed in the log (agreement 4.2 "Logs").
 * Ids are always preferred; these masks are for data that has no id yet.
 */
public enum LogMask {

    /** First 3 letters of each word, never the whole word: "Antony Palacios" -> "Ant*** Pal***". */
    NAME {
        @Override
        public String apply(String value) {
            StringBuilder masked = new StringBuilder();
            for (String word : value.trim().split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }
                if (!masked.isEmpty()) {
                    masked.append(' ');
                }
                int visible = Math.min(3, word.length() - 1);
                masked.append(word, 0, visible).append(STARS);
            }
            return masked.isEmpty() ? STARS : masked.toString();
        }
    },

    /** Only the last 3 digits: "72345678" -> "*****678". */
    DNI {
        @Override
        public String apply(String value) {
            String dni = value.trim();
            if (dni.length() <= 3) {
                return STARS;
            }
            return "*".repeat(dni.length() - 3) + dni.substring(dni.length() - 3);
        }
    },

    /** First letter of the user and the whole domain: "usuario@gmail.com" -> "u***@gmail.com". */
    EMAIL {
        @Override
        public String apply(String value) {
            String email = value.trim();
            int at = email.indexOf('@');
            if (at <= 0 || at == email.length() - 1) {
                return STARS;
            }
            return email.charAt(0) + STARS + email.substring(at);
        }
    },

    /** Scores, tokens and anything else that must never be logged, not even partially. */
    HIDDEN {
        @Override
        public String apply(String value) {
            return STARS;
        }
    };

    private static final String STARS = "***";

    public abstract String apply(String value);
}
