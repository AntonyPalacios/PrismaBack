package pe.com.edu.prisma.students.impl;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.StudentMergeService;
import pe.com.edu.prisma.students.StudentService;
import pe.com.edu.prisma.students.dto.MergePreviewDto;
import pe.com.edu.prisma.students.dto.MergePreviewDto.PersonalData;
import pe.com.edu.prisma.students.dto.MergePreviewDto.SharedStage;
import pe.com.edu.prisma.students.dto.MergePreviewDto.Side;
import pe.com.edu.prisma.students.dto.StudentDto;

/**
 * Moves everything of the removed student to the kept one, row by row in
 * SQL (agreements of 2026-11-18), and deletes the removed student for good:
 * <ul>
 * <li>an enrollment in a stage the kept student lacks moves as is, with its
 * tutor history, results and goals;</li>
 * <li>in a shared stage the kept enrollment stays: results move to it (the
 * same exam keeps the better result: attended over "no asistió", then the
 * higher total score; a tie keeps the kept student's), goals move unless it
 * has the same exam and area, its tutor history stays and the other's is
 * dropped, and a missing career is filled;</li>
 * <li>then one exam type per mock exam, by the same rule;</li>
 * <li>logbook entries and import rows move, guardians are linked once;</li>
 * <li>personal data stays the kept student's, blanks filled from the other;
 * names never change.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class StudentMergeServiceImpl implements StudentMergeService {

    static final String SAME_STUDENT = "Elige otro alumno: no se puede fusionar un alumno consigo mismo.";
    static final String STUDENT_NOT_FOUND = "No encontramos el alumno solicitado.";

    private static final Logger log = LoggerFactory.getLogger(StudentMergeServiceImpl.class);

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final StudentService studentService;
    private final CurrentUser currentUser;

    public StudentMergeServiceImpl(JdbcTemplate jdbc, EntityManager entityManager, StudentService studentService,
            CurrentUser currentUser) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.studentService = studentService;
        this.currentUser = currentUser;
    }

    /** One student's own data, as stored. */
    private record Person(long id, String firstName, String lastNamePat, String lastNameMat, boolean active,
            String dni, String email, String phone, LocalDate birthDate) {

        String fullName() {
            String lastNames = Stream.of(lastNamePat, lastNameMat).filter(Objects::nonNull).filter(p -> !p.isBlank())
                    .collect(Collectors.joining(" "));
            return lastNames + ", " + firstName;
        }

        PersonalData data() {
            return new PersonalData(dni, email, phone, birthDate);
        }
    }

    /** An enrollment: its stage, career and whether it is active. */
    private record Enrollment(long id, long stageId, Long careerId, boolean active) {
    }

    /** A result: its exam and how it went. */
    private record Result(long id, long examId, long mockExamId, boolean attended, BigDecimal totalScore) {
    }

    @Override
    public MergePreviewDto preview(Long keepId, Long removeId) {
        Person keep = person(keepId);
        Person remove = person(removeId);
        if (keep.id() == remove.id()) {
            throw new InvalidRequestException(SAME_STUDENT);
        }
        Map<Long, Enrollment> keepStages = enrollments(keep.id());
        List<SharedStage> shared = new ArrayList<>();
        int resultConflicts = 0;
        int goalConflicts = 0;
        for (Enrollment theirs : enrollments(remove.id()).values()) {
            Enrollment mine = keepStages.get(theirs.stageId());
            if (mine == null) {
                continue;
            }
            shared.add(new SharedStage(stageLabel(theirs.stageId()), currentTutor(mine.id()), currentTutor(theirs.id())));
            resultConflicts += count("""
                    SELECT count(*) FROM exam_result r WHERE r.student_stage_id = ?
                    AND EXISTS (SELECT 1 FROM exam_result k WHERE k.student_stage_id = ? AND k.exam_id = r.exam_id)""",
                    theirs.id(), mine.id());
            goalConflicts += count("""
                    SELECT count(*) FROM goal g WHERE g.student_stage_id = ?
                    AND EXISTS (SELECT 1 FROM goal k WHERE k.student_stage_id = ? AND k.target_exam_id = g.target_exam_id
                                AND k.area_id = g.area_id)""", theirs.id(), mine.id());
        }
        return new MergePreviewDto(side(keep), side(remove), shared, resultConflicts, goalConflicts, merged(keep, remove));
    }

    @Override
    @Transactional
    public StudentDto merge(Long keepId, Long removeId) {
        entityManager.flush(); // what JPA holds goes first: from here on it is plain SQL
        Person keep = person(keepId);
        Person remove = person(removeId);
        if (keep.id() == remove.id()) {
            throw new InvalidRequestException(SAME_STUDENT);
        }

        Map<Long, Enrollment> keepStages = enrollments(keep.id());
        int movedEnrollments = 0;
        int mergedEnrollments = 0;
        int droppedResults = 0;
        for (Enrollment theirs : enrollments(remove.id()).values()) {
            Enrollment mine = keepStages.get(theirs.stageId());
            if (mine == null) {
                // a stage the kept student lacks: the enrollment moves with everything it carries
                jdbc.update("UPDATE student_stage SET student_id = ? WHERE id = ?", keep.id(), theirs.id());
                movedEnrollments++;
                continue;
            }
            // a shared stage: the kept enrollment receives the other's results and goals
            for (Result result : results("r.student_stage_id = ?", theirs.id())) {
                List<Result> same = results("r.student_stage_id = ? AND r.exam_id = ?", mine.id(), result.examId());
                if (same.isEmpty()) {
                    jdbc.update("UPDATE exam_result SET student_stage_id = ? WHERE id = ?", mine.id(), result.id());
                } else if (better(result, same.get(0))) {
                    jdbc.update("DELETE FROM exam_result WHERE id = ?", same.get(0).id());
                    jdbc.update("UPDATE exam_result SET student_stage_id = ? WHERE id = ?", mine.id(), result.id());
                    droppedResults++;
                } else {
                    jdbc.update("DELETE FROM exam_result WHERE id = ?", result.id());
                    droppedResults++;
                }
            }
            jdbc.update("""
                    DELETE FROM goal g WHERE g.student_stage_id = ?
                    AND EXISTS (SELECT 1 FROM goal k WHERE k.student_stage_id = ? AND k.target_exam_id = g.target_exam_id
                                AND k.area_id = g.area_id)""", theirs.id(), mine.id());
            jdbc.update("UPDATE goal SET student_stage_id = ? WHERE student_stage_id = ?", mine.id(), theirs.id());
            // the kept tutor history stays; the duplicate's is dropped (an exception to the immutable history)
            if (count("SELECT count(*) FROM tutor_assignment WHERE student_stage_id = ?", mine.id()) > 0) {
                jdbc.update("DELETE FROM tutor_assignment WHERE student_stage_id = ?", theirs.id());
            } else {
                jdbc.update("UPDATE tutor_assignment SET student_stage_id = ? WHERE student_stage_id = ?", mine.id(), theirs.id());
            }
            if (mine.careerId() == null && theirs.careerId() != null) {
                jdbc.update("UPDATE student_stage SET career_id = ? WHERE id = ?", theirs.careerId(), mine.id());
            }
            jdbc.update("DELETE FROM student_stage WHERE id = ?", theirs.id());
            mergedEnrollments++;
        }

        droppedResults += oneTypePerMockExam(keep.id());

        int logEntries = jdbc.update("UPDATE log_entry SET student_id = ? WHERE student_id = ?", keep.id(), remove.id());
        jdbc.update("UPDATE import_row SET matched_student_id = ? WHERE matched_student_id = ?", keep.id(), remove.id());
        jdbc.update("""
                INSERT INTO student_parent (student_id, parent_id, relationship_type)
                SELECT ?, sp.parent_id, sp.relationship_type FROM student_parent sp
                WHERE sp.student_id = ?
                AND NOT EXISTS (SELECT 1 FROM student_parent k WHERE k.student_id = ? AND k.parent_id = sp.parent_id)""",
                keep.id(), remove.id(), keep.id());

        // the duplicate goes first: its DNI may be the one the kept student takes
        PersonalData data = merged(keep, remove);
        jdbc.update("DELETE FROM student_parent WHERE student_id = ?", remove.id());
        jdbc.update("DELETE FROM student WHERE id = ?", remove.id());
        jdbc.update("UPDATE student SET dni = ?, email = ?, phone = ?, birth_date = ?, updated_at = now(), updated_by = ? "
                + "WHERE id = ?", data.dni(), data.email(), data.phone(),
                data.birthDate() == null ? null : Date.valueOf(data.birthDate()), currentUser.id(), keep.id());

        entityManager.clear(); // JPA must not keep what SQL changed
        log.info("Students merged: keptId={} removedId={} enrollmentsMoved={} enrollmentsMerged={} resultsDropped={} "
                + "logEntriesMoved={}", keep.id(), remove.id(), movedEnrollments, mergedEnrollments, droppedResults, logEntries);
        return studentService.findById(keep.id());
    }

    /** One exam type per mock exam: of several results in the same mock exam the better one stays. */
    private int oneTypePerMockExam(long studentId) {
        Map<Long, List<Result>> byMockExam = results("s.student_id = ?", studentId).stream()
                .collect(Collectors.groupingBy(Result::mockExamId));
        int dropped = 0;
        for (List<Result> results : byMockExam.values()) {
            if (results.size() < 2) {
                continue;
            }
            Result best = results.stream().reduce((a, b) -> better(b, a) ? b : a).orElseThrow();
            for (Result result : results) {
                if (result.id() != best.id()) {
                    jdbc.update("DELETE FROM exam_result WHERE id = ?", result.id());
                    dropped++;
                }
            }
        }
        return dropped;
    }

    /** {@code candidate} beats {@code current}: attended over "no asistió", then a higher total score (a tie keeps current). */
    private static boolean better(Result candidate, Result current) {
        if (candidate.attended() != current.attended()) {
            return candidate.attended();
        }
        BigDecimal mine = candidate.totalScore() == null ? BigDecimal.ZERO : candidate.totalScore();
        BigDecimal theirs = current.totalScore() == null ? BigDecimal.ZERO : current.totalScore();
        return candidate.attended() && mine.compareTo(theirs) > 0;
    }

    /** The kept student's data, its blanks filled from the other's. */
    private static PersonalData merged(Person keep, Person remove) {
        return new PersonalData(
                keep.dni() != null ? keep.dni() : remove.dni(),
                keep.email() != null ? keep.email() : remove.email(),
                keep.phone() != null ? keep.phone() : remove.phone(),
                keep.birthDate() != null ? keep.birthDate() : remove.birthDate());
    }

    private Side side(Person person) {
        long id = person.id();
        return new Side(id, person.fullName(), person.active(), person.data(),
                count("SELECT count(*) FROM student_stage WHERE student_id = ?", id),
                count("SELECT count(*) FROM exam_result r JOIN student_stage s ON s.id = r.student_stage_id "
                        + "WHERE s.student_id = ?", id),
                count("SELECT count(*) FROM goal g JOIN student_stage s ON s.id = g.student_stage_id WHERE s.student_id = ?", id),
                count("SELECT count(*) FROM log_entry WHERE student_id = ?", id),
                count("SELECT count(*) FROM student_parent WHERE student_id = ?", id));
    }

    private Person person(Long id) {
        List<Person> found = jdbc.query("""
                SELECT id, first_name, last_name_pat, last_name_mat, active, dni, email, phone, birth_date
                FROM student WHERE id = ?""", (rs, n) -> new Person(rs.getLong("id"), rs.getString("first_name"),
                rs.getString("last_name_pat"), rs.getString("last_name_mat"), rs.getBoolean("active"), rs.getString("dni"),
                rs.getString("email"), rs.getString("phone"),
                rs.getDate("birth_date") == null ? null : rs.getDate("birth_date").toLocalDate()), id);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
        return found.get(0);
    }

    /** A student's enrollments by stage. */
    private Map<Long, Enrollment> enrollments(long studentId) {
        Map<Long, Enrollment> byStage = new HashMap<>();
        jdbc.query("SELECT id, stage_id, career_id, active FROM student_stage WHERE student_id = ? ORDER BY id",
                rs -> {
                    Long careerId = rs.getObject("career_id", Long.class);
                    byStage.put(rs.getLong("stage_id"), new Enrollment(rs.getLong("id"), rs.getLong("stage_id"),
                            careerId, rs.getBoolean("active")));
                }, studentId);
        return byStage;
    }

    private List<Result> results(String where, Object... args) {
        return jdbc.query("""
                SELECT r.id, r.exam_id, e.mock_exam_id, r.attended, r.total_score
                FROM exam_result r JOIN exam e ON e.id = r.exam_id JOIN student_stage s ON s.id = r.student_stage_id
                WHERE\s""" + where + " ORDER BY r.id",
                (rs, n) -> new Result(rs.getLong("id"), rs.getLong("exam_id"), rs.getLong("mock_exam_id"),
                        rs.getBoolean("attended"), rs.getBigDecimal("total_score")), args);
    }

    private String stageLabel(long stageId) {
        return jdbc.queryForObject("SELECT c.name || ' · ' || s.name FROM stage s JOIN cycle c ON c.id = s.cycle_id "
                + "WHERE s.id = ?", String.class, stageId);
    }

    /** "Apellidos, Nombres" of the enrollment's open tutor assignment, or null. */
    private String currentTutor(long enrollmentId) {
        List<String> names = jdbc.query("""
                SELECT u.last_name_pat, u.last_name_mat, u.first_name FROM tutor_assignment a JOIN app_user u ON u.id = a.tutor_id
                WHERE a.student_stage_id = ? AND a.valid_to IS NULL""", (rs, n) -> Stream.of(rs.getString(1), rs.getString(2))
                .filter(Objects::nonNull).filter(p -> !p.isBlank()).collect(Collectors.joining(" ")) + ", " + rs.getString(3),
                enrollmentId);
        return names.isEmpty() ? null : names.get(0);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
