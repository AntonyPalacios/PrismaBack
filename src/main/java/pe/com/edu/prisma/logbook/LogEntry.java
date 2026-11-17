package pe.com.edu.prisma.logbook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.BaseEntity;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.users.User;

/**
 * One counseling session in a student's logbook (REQ-LOGBOOK-01). Immutable
 * by design: there is no update or delete, a mistake is fixed with a new
 * entry. It belongs to the student, not to an enrollment, so the history
 * spans every stage and tutor. The {@code extra} JSON column is not mapped
 * yet: nothing writes it.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "log_entry")
public class LogEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Column(name = "session_date", nullable = false, updatable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_with", nullable = false, updatable = false, length = 10)
    private MeetingWith meetingWith;

    @Column(name = "motivo", updatable = false)
    private String reason;

    @Column(name = "acuerdos", updatable = false)
    private String agreements;

    @Column(name = "observaciones", updatable = false)
    private String observations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
