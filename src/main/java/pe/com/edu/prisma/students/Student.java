package pe.com.edu.prisma.students;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * A student's personal file: the base for enrollment, exams, goals and the
 * logbook. Students are minors, so every field here except the id is
 * personal data (Ley 29733): it never goes to the logs in clear text.
 * Students are never physically deleted; retiring one flips {@code active}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student")
public class Student extends FullAuditEntity {

    @Column(name = "first_name", nullable = false, length = 150)
    private String firstName;

    @Column(name = "last_name_pat", nullable = false, length = 100)
    private String lastNamePat;

    @Column(name = "last_name_mat", length = 100)
    private String lastNameMat;

    // optional, but unique when present: ux_student_dni (V1) and the service
    @Column(length = 20)
    private String dni;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false)
    private boolean active = true;
}
