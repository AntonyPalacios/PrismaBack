package pe.com.edu.prisma.common;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Every entity in the schema uses a BIGSERIAL surrogate key — sharing the
 * id field here keeps repositories' generic {@code findById(Long)} usable
 * across modules without repeating this boilerplate per entity.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
