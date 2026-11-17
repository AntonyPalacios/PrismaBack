package pe.com.edu.prisma.students;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A guardian linked to a student, with the relationship (REQ-STUDENTS-03). Unlinking deletes only this row. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student_parent")
public class StudentParent {

    @EmbeddedId
    private Key id;

    @MapsId("studentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @MapsId("parentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 20)
    private RelationshipType relationshipType;

    public StudentParent(Student student, Parent parent, RelationshipType relationshipType) {
        this.id = new Key(student.getId(), parent.getId());
        this.student = student;
        this.parent = parent;
        this.relationshipType = relationshipType;
    }

    @Embeddable
    public record Key(
            @Column(name = "student_id") Long studentId,
            @Column(name = "parent_id") Long parentId
    ) implements Serializable {
    }
}
