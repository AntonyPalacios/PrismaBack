package pe.com.edu.prisma.students;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * A guardian (REQ-STUDENTS-03): one person who can look after several
 * students (siblings). The name is a single field, shown as typed. Never
 * deleted: unlinking from a student keeps them.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "parent")
public class Parent extends FullAuditEntity {

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 30)
    private String phone;

    private String email;
}
