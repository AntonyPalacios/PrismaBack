package pe.com.edu.prisma.imports;

/**
 * How a row stands. AUTO_MATCHED / MANUAL_MATCHED: it is an existing student
 * (found by the system / chosen by a person). NEW_STUDENT: it will be
 * created. UNRESOLVED: a person must decide. IGNORED: left out of the batch.
 */
public enum ImportRowStatus {
    AUTO_MATCHED,
    MANUAL_MATCHED,
    NEW_STUDENT,
    UNRESOLVED,
    IGNORED
}
