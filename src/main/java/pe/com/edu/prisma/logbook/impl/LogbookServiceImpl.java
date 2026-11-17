package pe.com.edu.prisma.logbook.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.common.exception.ConfirmationRequiredException;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.logbook.LogEntry;
import pe.com.edu.prisma.logbook.LogEntryRepository;
import pe.com.edu.prisma.logbook.LogbookService;
import pe.com.edu.prisma.logbook.dto.CreateLogEntryRequest;
import pe.com.edu.prisma.logbook.dto.LogEntryDto;
import pe.com.edu.prisma.logbook.dto.LogEntryPageDto;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.CurrentEnrollments;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

/**
 * A student's logbook (REQ-LOGBOOK-01/02, agreements of 2026-11-17). Only the
 * student's current tutor writes (an admin reads, but never writes on a
 * tutor's behalf); ADMIN and the current tutor read the whole history, of
 * every stage and author. A former tutor loses access. Entries are never
 * updated or deleted.
 */
@Service
@Transactional(readOnly = true)
public class LogbookServiceImpl implements LogbookService {

    static final String STUDENT_NOT_FOUND = "No encontramos al alumno solicitado.";
    static final String NOT_THEIR_TUTOR_READ = "Solo su tutor actual y el administrador pueden ver la bitácora de este alumno.";
    static final String NOT_THEIR_TUTOR_WRITE = "Solo el tutor actual del alumno puede registrar entradas en su bitácora.";
    static final String ALL_EMPTY = "Escribe al menos el motivo, los compromisos o las observaciones de la sesión.";
    static final String FUTURE_DATE = "La fecha de la sesión no puede ser futura.";
    static final String SOME_EMPTY = "Quedaron vacíos: %s. ¿Guardar la entrada así? Después no se podrá editar ni borrar.";

    private static final Logger log = LoggerFactory.getLogger(LogbookServiceImpl.class);
    private static final int MAX_SIZE = 100;

    private final LogEntryRepository logEntryRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final CurrentEnrollments currentEnrollments;
    private final CurrentUser currentUser;
    private final Clock clock;

    public LogbookServiceImpl(LogEntryRepository logEntryRepository, StudentRepository studentRepository,
            UserRepository userRepository, CurrentEnrollments currentEnrollments, CurrentUser currentUser, Clock clock) {
        this.logEntryRepository = logEntryRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.currentEnrollments = currentEnrollments;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public LogEntryPageDto findByStudent(Long studentId, int page, int size) {
        getStudent(studentId);
        if (!currentUser.hasRole(Role.ADMIN) && !isCurrentTutor(studentId)) {
            throw new AccessDeniedException(NOT_THEIR_TUTOR_READ);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Page<LogEntry> found = logEntryRepository.findByStudentIdOrderBySessionDateDescIdDesc(studentId,
                PageRequest.of(safePage, safeSize));
        return new LogEntryPageDto(found.getContent().stream().map(LogbookServiceImpl::toDto).toList(),
                safePage, safeSize, found.getTotalElements(), found.hasNext());
    }

    @Override
    @Transactional
    public LogEntryDto create(Long studentId, CreateLogEntryRequest request) {
        Student student = getStudent(studentId);
        // not even an admin: the entry is the current tutor's own record of the session
        if (!isCurrentTutor(studentId)) {
            throw new AccessDeniedException(NOT_THEIR_TUTOR_WRITE);
        }
        if (request.sessionDate().isAfter(LocalDate.now(clock))) {
            throw new InvalidRequestException(FUTURE_DATE);
        }
        String reason = blankToNull(request.reason());
        String agreements = blankToNull(request.agreements());
        String observations = blankToNull(request.observations());
        if (reason == null && agreements == null && observations == null) {
            throw new InvalidRequestException(ALL_EMPTY);
        }
        // observations are optional for good: like the academy's sheet, an entry is reason + agreements
        List<String> empty = new ArrayList<>();
        if (reason == null) empty.add("reason");
        if (agreements == null) empty.add("agreements");
        if (!empty.isEmpty() && !request.confirm()) {
            String labels = empty.stream().map(LogbookServiceImpl::label).collect(Collectors.joining(", "));
            throw new ConfirmationRequiredException(SOME_EMPTY.formatted(labels), Map.of("emptyFields", empty));
        }

        LogEntry entry = new LogEntry();
        entry.setStudent(student);
        entry.setAuthor(userRepository.getReferenceById(currentUser.id()));
        entry.setSessionDate(request.sessionDate());
        entry.setMeetingWith(request.meetingWith());
        entry.setReason(reason);
        entry.setAgreements(agreements);
        entry.setObservations(observations);
        entry.setCreatedAt(OffsetDateTime.now(clock));
        LogEntry saved = logEntryRepository.saveAndFlush(entry);
        log.info("Logbook entry created: studentId={}, entryId={}", studentId, saved.getId()); // never its text
        return toDto(logEntryRepository.findById(saved.getId()).orElseThrow());
    }

    private boolean isCurrentTutor(Long studentId) {
        Long tutorId = currentEnrollments.of(studentId).map(CurrentEnrollments.Current::tutorId).orElse(null);
        return tutorId != null && Objects.equals(tutorId, currentUser.id());
    }

    private Student getStudent(Long studentId) {
        return studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
    }

    private static String label(String field) {
        return switch (field) {
            case "reason" -> "motivo";
            default -> "compromisos / recomendaciones";
        };
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    private static LogEntryDto toDto(LogEntry entry) {
        User author = entry.getAuthor();
        return new LogEntryDto(entry.getId(), entry.getStudent().getId(), author.getId(), displayName(author),
                entry.getSessionDate(), entry.getMeetingWith(), entry.getCreatedAt(), entry.getReason(), entry.getAgreements(),
                entry.getObservations());
    }

    // "Apellidos, Nombres"
    private static String displayName(User user) {
        String lastNames = Stream.of(user.getLastNamePat(), user.getLastNameMat()).filter(Objects::nonNull)
                .filter(part -> !part.isBlank()).collect(Collectors.joining(" "));
        return lastNames + ", " + user.getFirstName();
    }
}
