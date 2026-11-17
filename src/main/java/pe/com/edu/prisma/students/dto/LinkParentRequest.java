package pe.com.edu.prisma.students.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import pe.com.edu.prisma.students.RelationshipType;

/**
 * Links a guardian to a student: an existing one ({@code parentId}) or a new
 * one registered at once ({@code parent}), exactly one of the two.
 */
public record LinkParentRequest(
        Long parentId,

        @Valid
        UpsertParentRequest parent,

        @NotNull(message = "Elige el parentesco.")
        RelationshipType relationshipType
) {
}
