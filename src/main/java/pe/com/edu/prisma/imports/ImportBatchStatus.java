package pe.com.edu.prisma.imports;

/** A batch waits for review, and ends confirmed (its data became real) or discarded. */
public enum ImportBatchStatus {
    PENDING_REVIEW,
    CONFIRMED,
    DISCARDED,
    SUPERSEDED
}
