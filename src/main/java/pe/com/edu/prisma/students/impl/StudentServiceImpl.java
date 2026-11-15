package pe.com.edu.prisma.students.impl;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.CurrentEnrollments;
import pe.com.edu.prisma.students.CurrentEnrollments.Current;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.students.StudentService;
import pe.com.edu.prisma.students.dto.StudentDto;
import pe.com.edu.prisma.students.dto.UpsertStudentRequest;
import pe.com.edu.prisma.students.dto.StudentFilter;
import pe.com.edu.prisma.students.mapper.StudentMapper;
import pe.com.edu.prisma.users.Role;

@Service
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    static final String DUPLICATE_DNI = "Ya existe un alumno con ese DNI.";
    static final String ALREADY_INACTIVE = "Este alumno ya está dado de baja.";
    static final String ALREADY_ACTIVE = "Este alumno ya está activo.";
    static final String INACTIVE_NOT_EDITABLE = "Este alumno está dado de baja: reactívalo para editar sus datos.";

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    // "Apellidos, Nombres": paternal surname, maternal surname (empty last), then first names
    private static final Comparator<Student> ORDER = SpanishOrder.<Student>by(Student::getLastNamePat)
            .thenComparing(SpanishOrder.by(Student::getLastNameMat))
            .thenComparing(SpanishOrder.by(Student::getFirstName))
            .thenComparing(Student::getId);

    static final String ONLY_ADMIN_FILTERS = "Solo el administrador puede filtrar alumnos por tutor.";
    static final String NOT_THEIR_TUTOR = "Solo el administrador o el tutor actual del alumno pueden editar sus datos.";

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final CurrentEnrollments currentEnrollments;
    private final CurrentUser currentUser;

    public StudentServiceImpl(StudentRepository studentRepository, StudentMapper studentMapper,
            CurrentEnrollments currentEnrollments, CurrentUser currentUser) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
        this.currentEnrollments = currentEnrollments;
        this.currentUser = currentUser;
    }

    // The academy has a few hundred students: they are filtered and sorted in
    // memory, which is what lets the search ignore case and accents the same
    // way the order does. Revisit (pagination, SQL search) if that changes.
    @Override
    public List<StudentDto> findAll(StudentFilter filter) {
        boolean admin = currentUser.hasRole(Role.ADMIN);
        boolean filtersByTutor = filter.tutorId() != null || filter.withoutTutor();
        if (filtersByTutor && !admin) {
            throw new AccessDeniedException(ONLY_ADMIN_FILTERS);
        }
        // a tutor (who is not also an admin) only lists the students assigned to them today
        Long tutorId = !admin && currentUser.hasRole(Role.TUTOR) ? myId() : filter.tutorId();

        String wanted = normalize(filter.search());
        Map<Long, Current> current = currentEnrollments.byStudent();
        List<Student> students = filter.includeInactive() ? studentRepository.findAll() : studentRepository.findByActiveTrue();
        return students.stream()
                .filter(student -> wanted.isEmpty() || matches(student, wanted))
                .filter(student -> tutorId == null || tutorId.equals(tutorOf(current.get(student.getId()))))
                .filter(student -> !filter.withoutTutor() || tutorOf(current.get(student.getId())) == null)
                .sorted(ORDER)
                .map(student -> studentMapper.toDto(student, current.get(student.getId())))
                .toList();
    }

    @Override
    public StudentDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public StudentDto create(UpsertStudentRequest request) {
        if (request.dni() != null && studentRepository.existsByDni(request.dni())) {
            throw new BusinessRuleException(DUPLICATE_DNI);
        }
        Student student = new Student(); // born active
        apply(student, request);
        Student saved = studentRepository.save(student);
        log.info("Student created: studentId={}", saved.getId()); // only the id: the rest is personal data
        return studentMapper.toDto(saved, null); // born without enrollment
    }

    @Override
    @Transactional
    public StudentDto update(Long id, UpsertStudentRequest request) {
        Student student = getOrThrow(id);
        Current current = currentEnrollments.of(id).orElse(null);
        // only an admin or the student's current tutor; no exception for whoever registered them
        if (!currentUser.hasRole(Role.ADMIN) && !Objects.equals(myId(), tutorOf(current))) {
            throw new AccessDeniedException(NOT_THEIR_TUTOR);
        }
        if (!student.isActive()) {
            throw new BusinessRuleException(INACTIVE_NOT_EDITABLE);
        }
        if (request.dni() != null && studentRepository.existsByDniAndIdNot(request.dni(), id)) {
            throw new BusinessRuleException(DUPLICATE_DNI);
        }
        apply(student, request);
        log.info("Student updated: studentId={}", id);
        return studentMapper.toDto(student, current);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Student student = getOrThrow(id);
        if (!student.isActive()) {
            throw new BusinessRuleException(ALREADY_INACTIVE);
        }
        student.setActive(false); // exams, goals, logbook and enrollments stay as they are
        log.info("Student deactivated: studentId={}", id);
    }

    @Override
    @Transactional
    public StudentDto reactivate(Long id) {
        Student student = getOrThrow(id);
        if (student.isActive()) {
            throw new BusinessRuleException(ALREADY_ACTIVE);
        }
        student.setActive(true);
        log.info("Student reactivated: studentId={}", id);
        return toDto(student);
    }

    /** Every word typed must appear in the student's names or DNI ("palacios ant", "7234"). */
    private static boolean matches(Student student, String wanted) {
        String haystack = normalize(String.join(" ",
                Stream.of(student.getLastNamePat(), student.getLastNameMat(), student.getFirstName(), student.getDni())
                        .filter(value -> value != null)
                        .toList()));
        return Stream.of(wanted.split(" ")).allMatch(haystack::contains);
    }

    /** Lower case, no accents, single spaces: "  Álvarez   PÉREZ " → "alvarez perez". */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    // optional fields already arrive trimmed, with "" turned into null (UpsertStudentRequest)
    private static void apply(Student student, UpsertStudentRequest request) {
        student.setFirstName(request.firstName().trim());
        student.setLastNamePat(request.lastNamePat().trim());
        student.setLastNameMat(request.lastNameMat());
        student.setDni(request.dni());
        student.setEmail(request.email());
        student.setPhone(request.phone());
        student.setBirthDate(request.birthDate());
    }

    private StudentDto toDto(Student student) {
        return studentMapper.toDto(student, currentEnrollments.of(student.getId()).orElse(null));
    }

    private static Long tutorOf(Current current) {
        return current == null ? null : current.tutorId();
    }

    /** The caller's id; a session without one (never in practice) matches no tutor. */
    private Long myId() {
        Long id = currentUser.id();
        return id == null ? -1L : id;
    }

    private Student getOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el alumno solicitado."));
    }
}
