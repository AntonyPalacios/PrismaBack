package pe.com.edu.prisma.students.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.CurrentEnrollments;
import pe.com.edu.prisma.students.CurrentEnrollments.Current;
import pe.com.edu.prisma.students.Parent;
import pe.com.edu.prisma.students.ParentRepository;
import pe.com.edu.prisma.students.ParentService;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.StudentParent;
import pe.com.edu.prisma.students.StudentParentRepository;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.students.dto.LinkParentRequest;
import pe.com.edu.prisma.students.dto.ParentDto;
import pe.com.edu.prisma.students.dto.StudentParentDto;
import pe.com.edu.prisma.students.dto.UpsertParentRequest;
import pe.com.edu.prisma.users.Role;

@Service
@Transactional(readOnly = true)
public class ParentServiceImpl implements ParentService {

    static final String STUDENT_NOT_FOUND = "No encontramos el alumno solicitado.";
    static final String PARENT_NOT_FOUND = "No encontramos el apoderado solicitado.";
    static final String LINK_NOT_FOUND = "Este apoderado no está vinculado al alumno.";
    static final String ALREADY_LINKED = "Este apoderado ya está vinculado al alumno.";
    static final String ONE_PARENT = "Elige un apoderado registrado o registra uno nuevo, no ambos.";
    static final String INACTIVE_STUDENT = "Este alumno está dado de baja: reactívalo para cambiar sus apoderados.";
    static final String NOT_THEIR_TUTOR = "Solo el administrador o el tutor actual del alumno pueden cambiar sus apoderados.";

    private static final Logger log = LoggerFactory.getLogger(ParentServiceImpl.class);

    private static final Comparator<StudentParentDto> BY_NAME =
            SpanishOrder.<StudentParentDto>by(StudentParentDto::fullName).thenComparing(StudentParentDto::parentId);
    private static final Comparator<Student> STUDENT_ORDER = SpanishOrder.<Student>by(Student::getLastNamePat)
            .thenComparing(SpanishOrder.by(Student::getLastNameMat))
            .thenComparing(SpanishOrder.by(Student::getFirstName))
            .thenComparing(Student::getId);

    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentRepository studentRepository;
    private final CurrentEnrollments currentEnrollments;
    private final CurrentUser currentUser;

    public ParentServiceImpl(ParentRepository parentRepository, StudentParentRepository studentParentRepository,
            StudentRepository studentRepository, CurrentEnrollments currentEnrollments, CurrentUser currentUser) {
        this.parentRepository = parentRepository;
        this.studentParentRepository = studentParentRepository;
        this.studentRepository = studentRepository;
        this.currentEnrollments = currentEnrollments;
        this.currentUser = currentUser;
    }

    @Override
    public List<ParentDto> search(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }
        String escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String digits = trimmed.replaceAll("[^0-9]", "");
        return parentRepository.search("%" + escaped + "%", digits, "%" + digits + "%").stream()
                .map(ParentServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParentDto create(UpsertParentRequest request) {
        Parent saved = parentRepository.save(apply(new Parent(), request));
        log.info("Guardian created: parentId={}", saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ParentDto update(Long id, UpsertParentRequest request) {
        Parent parent = parentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(PARENT_NOT_FOUND));
        if (!currentUser.hasRole(Role.ADMIN)) {
            // the current tutor of any of their students: it is the same person for all of them
            Map<Long, Current> current = currentEnrollments.byStudent();
            boolean tutorsOne = studentParentRepository.findByParentIdIn(List.of(id)).stream()
                    .map(link -> current.get(link.getStudent().getId()))
                    .anyMatch(c -> c != null && Objects.equals(c.tutorId(), currentUser.id()));
            if (!tutorsOne) {
                throw new AccessDeniedException(NOT_THEIR_TUTOR);
            }
        }
        apply(parent, request);
        log.info("Guardian updated: parentId={}", id);
        return toDto(parent);
    }

    @Override
    public List<StudentParentDto> findByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
        List<StudentParent> links = studentParentRepository.findByStudentId(studentId);
        Map<Long, List<String>> others = otherStudents(studentId,
                links.stream().map(link -> link.getParent().getId()).collect(Collectors.toSet()));
        return links.stream()
                .map(link -> toDto(link, others.getOrDefault(link.getParent().getId(), List.of())))
                .sorted(BY_NAME)
                .toList();
    }

    @Override
    @Transactional
    public StudentParentDto link(Long studentId, LinkParentRequest request) {
        if ((request.parentId() == null) == (request.parent() == null)) {
            throw new InvalidRequestException(ONE_PARENT);
        }
        Student student = getEditableStudent(studentId);
        Parent parent;
        if (request.parentId() != null) {
            parent = parentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException(PARENT_NOT_FOUND));
            if (studentParentRepository.existsById(new StudentParent.Key(studentId, parent.getId()))) {
                throw new BusinessRuleException(ALREADY_LINKED);
            }
        } else {
            parent = parentRepository.save(apply(new Parent(), request.parent()));
            log.info("Guardian created: parentId={}", parent.getId());
        }
        StudentParent link = studentParentRepository.save(new StudentParent(student, parent, request.relationshipType()));
        log.info("Guardian linked: studentId={} parentId={} relationship={}",
                studentId, parent.getId(), request.relationshipType());
        return toDto(link, otherStudents(studentId, Set.of(parent.getId())).getOrDefault(parent.getId(), List.of()));
    }

    @Override
    @Transactional
    public void unlink(Long studentId, Long parentId) {
        getEditableStudent(studentId);
        StudentParent link = studentParentRepository.findById(new StudentParent.Key(studentId, parentId))
                .orElseThrow(() -> new ResourceNotFoundException(LINK_NOT_FOUND));
        studentParentRepository.delete(link); // the guardian and their other links stay
        log.info("Guardian unlinked: studentId={} parentId={}", studentId, parentId);
    }

    /** Only an admin or the student's current tutor, and never on a deactivated student. */
    private Student getEditableStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        if (!currentUser.hasRole(Role.ADMIN)) {
            Long tutorId = currentEnrollments.of(studentId).map(Current::tutorId).orElse(null);
            if (tutorId == null || !tutorId.equals(currentUser.id())) {
                throw new AccessDeniedException(NOT_THEIR_TUTOR);
            }
        }
        if (!student.isActive()) {
            throw new BusinessRuleException(INACTIVE_STUDENT);
        }
        return student;
    }

    /** For each guardian, the other students they look after, by name. */
    private Map<Long, List<String>> otherStudents(Long studentId, Set<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return studentParentRepository.findByParentIdIn(parentIds).stream()
                .filter(link -> !link.getStudent().getId().equals(studentId))
                .collect(Collectors.groupingBy(link -> link.getParent().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                .map(StudentParent::getStudent)
                                .sorted(STUDENT_ORDER)
                                .map(ParentServiceImpl::displayName)
                                .toList())));
    }

    private static Parent apply(Parent parent, UpsertParentRequest request) {
        parent.setFullName(request.fullName());
        parent.setPhone(request.phone());
        parent.setEmail(request.email());
        return parent;
    }

    private static ParentDto toDto(Parent parent) {
        return new ParentDto(parent.getId(), parent.getFullName(), parent.getPhone(), parent.getEmail());
    }

    private static StudentParentDto toDto(StudentParent link, List<String> others) {
        Parent parent = link.getParent();
        return new StudentParentDto(parent.getId(), parent.getFullName(), parent.getPhone(), parent.getEmail(),
                link.getRelationshipType(), others);
    }

    // "Apellidos, Nombres"
    private static String displayName(Student student) {
        String lastNames = Stream.of(student.getLastNamePat(), student.getLastNameMat()).filter(Objects::nonNull)
                .filter(p -> !p.isBlank()).collect(Collectors.joining(" "));
        return lastNames + ", " + student.getFirstName();
    }
}
