package pe.com.edu.prisma.security;

/**
 * Contract the {@code Authentication#getPrincipal()} implements once the
 * JWT-cookie filter runs (see the security module). Kept as a narrow
 * interface — rather than depending on a concrete user-details class —
 * so {@code common} (auditing) doesn't need to depend on the security
 * package's internals.
 */
public interface AuthenticatedUser {

    Long getId();
}
