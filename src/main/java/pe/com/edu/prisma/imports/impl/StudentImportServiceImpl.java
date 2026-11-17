package pe.com.edu.prisma.imports.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.StageRepository;
import pe.com.edu.prisma.catalog.Career;
import pe.com.edu.prisma.catalog.CareerRepository;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.imports.ImportBatch;
import pe.com.edu.prisma.imports.ImportBatchRepository;
import pe.com.edu.prisma.imports.ImportBatchStatus;
import pe.com.edu.prisma.imports.ImportBatchType;
import pe.com.edu.prisma.imports.ImportRow;
import pe.com.edu.prisma.imports.ImportRowRepository;
import pe.com.edu.prisma.imports.ImportRowStatus;
import pe.com.edu.prisma.imports.StudentImportService;
import pe.com.edu.prisma.imports.StudentRowPayload;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;
import pe.com.edu.prisma.imports.dto.ImportRowDto;
import pe.com.edu.prisma.imports.dto.ResolveRowRequest;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.students.EnrollmentRepository;
import pe.com.edu.prisma.students.EnrollmentService;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.students.StudentService;
import pe.com.edu.prisma.students.dto.CreateEnrollmentRequest;
import pe.com.edu.prisma.students.dto.UpsertStudentRequest;
import pe.com.edu.prisma.tutoring.TutorAssignmentService;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static pe.com.edu.prisma.imports.impl.ImportNames.displayName;
import static pe.com.edu.prisma.imports.impl.ImportNames.fullName;
import static pe.com.edu.prisma.imports.impl.ImportNames.normalize;

@Service
@Transactional(readOnly = true)
public class StudentImportServiceImpl implements StudentImportService {

    static final String BATCH_NOT_FOUND = "No encontramos la importación solicitada.";
    static final String ROW_NOT_FOUND = "No encontramos esa fila en la importación.";
    static final String STAGE_NOT_FOUND = "No encontramos la etapa solicitada.";
    static final String STUDENT_NOT_FOUND = "No encontramos el alumno solicitado.";
    static final String CLOSED_CYCLE = "No se puede matricular en una etapa de un ciclo cerrado.";
    static final String NOT_PENDING = "Esta importación ya fue confirmada o descartada: ya no se puede cambiar.";
    static final String UNRESOLVED_LEFT = "Aún hay filas sin resolver: resuélvelas o ignóralas antes de confirmar.";
    static final String CHOOSE_STUDENT = "Elige el alumno con el que se empareja la fila.";
    static final String DNI_EXISTS = "Ya existe un alumno con ese DNI: empareja la fila con él.";
    static final String INACTIVE_STUDENT = "Este alumno está dado de baja: reactívalo antes de matricularlo, o ignora la fila.";
    static final String ALREADY_ENROLLED = "Ya está matriculado en esta etapa: se actualizan su carrera y su tutor si el archivo los trae.";
    static final String INACTIVE_NOTICE = "Está dado de baja: no se puede matricular. Reactívalo antes de confirmar o ignora la fila.";

    // how alike two full names must be to suggest "this may be the same person"
    private static final double SIMILAR_NAME = 0.92;
    private static final int MAX_CANDIDATES = 5;

    private static final Logger log = LoggerFactory.getLogger(StudentImportServiceImpl.class);

    private final ImportBatchRepository batchRepository;
    private final ImportRowRepository rowRepository;
    private final StudentSheet sheet;
    private final StudentRepository studentRepository;
    private final StageRepository stageRepository;
    private final CareerRepository careerRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final TutorAssignmentService tutorAssignmentService;
    private final CurrentUser currentUser;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public StudentImportServiceImpl(ImportBatchRepository batchRepository, ImportRowRepository rowRepository,
            StudentSheet sheet, StudentRepository studentRepository, StageRepository stageRepository,
            CareerRepository careerRepository, UserRepository userRepository, EnrollmentRepository enrollmentRepository,
            StudentService studentService, EnrollmentService enrollmentService,
            TutorAssignmentService tutorAssignmentService, CurrentUser currentUser, JsonMapper jsonMapper, Clock clock) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.sheet = sheet;
        this.studentRepository = studentRepository;
        this.stageRepository = stageRepository;
        this.careerRepository = careerRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
        this.tutorAssignmentService = tutorAssignmentService;
        this.currentUser = currentUser;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public byte[] template() {
        return sheet.template();
    }

    @Override
    @Transactional
    public ImportBatchDto upload(MultipartFile file, Long stageId) {
        Stage stage = stageId == null ? null : stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException(STAGE_NOT_FOUND));
        if (stage != null && stage.getCycle().getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_CYCLE);
        }
        List<StudentSheet.ParsedRow> parsed;
        try (InputStream in = file.getInputStream()) {
            parsed = sheet.read(in);
        } catch (IOException e) {
            throw new InvalidRequestException("No pudimos leer el archivo: vuelve a subirlo.");
        }

        ImportBatch batch = new ImportBatch();
        batch.setType(ImportBatchType.STUDENTS);
        batch.setStage(stage);
        batch.setFileName(Optional.ofNullable(file.getOriginalFilename()).orElse("alumnos.xlsx"));
        batch.setUploadedBy(currentUser.id());
        batch.setUploadedAt(OffsetDateTime.now(clock));
        batchRepository.save(batch);

        Matcher matcher = new Matcher(stage);
        for (StudentSheet.ParsedRow parsedRow : parsed) {
            batch.getRows().add(matcher.toRow(batch, parsedRow));
        }
        rowRepository.saveAll(batch.getRows());
        log.info("Student import uploaded: batchId={} stageId={} rows={}", batch.getId(), stageId, parsed.size());
        return toDto(batch, true);
    }

    @Override
    public List<ImportBatchDto> findAll(ImportBatchStatus status) {
        List<ImportBatch> batches = status == null
                ? batchRepository.findByTypeOrderByUploadedAtDescIdDesc(ImportBatchType.STUDENTS)
                : batchRepository.findByTypeAndStatusOrderByUploadedAtDescIdDesc(ImportBatchType.STUDENTS, status);
        return batches.stream().map(batch -> toDto(batch, false)).toList();
    }

    @Override
    public ImportBatchDto findById(Long id) {
        return toDto(getOrThrow(id), true);
    }

    @Override
    @Transactional
    public ImportBatchDto resolve(Long batchId, Long rowId, ResolveRowRequest request) {
        ImportBatch batch = getPending(batchId);
        ImportRow row = rowRepository.findByIdAndBatchId(rowId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException(ROW_NOT_FOUND));
        StudentRowPayload payload = payload(row);
        switch (request.action()) {
            case MATCH -> {
                if (request.studentId() == null) {
                    throw new InvalidRequestException(CHOOSE_STUDENT);
                }
                Student student = studentRepository.findById(request.studentId())
                        .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
                if (batch.getStage() != null && !student.isActive()) {
                    throw new BusinessRuleException(INACTIVE_STUDENT);
                }
                row.setMatchedStudent(student);
                row.setStatus(ImportRowStatus.MANUAL_MATCHED);
            }
            case NEW -> {
                if (payload.dni() != null && studentRepository.existsByDni(payload.dni())) {
                    throw new BusinessRuleException(DNI_EXISTS);
                }
                row.setMatchedStudent(null);
                row.setStatus(ImportRowStatus.NEW_STUDENT);
            }
            case IGNORE -> row.setStatus(ImportRowStatus.IGNORED);
        }
        row.setResolvedBy(currentUser.id());
        row.setResolvedAt(OffsetDateTime.now(clock));
        log.info("Import row resolved: batchId={} rowId={} status={}", batchId, rowId, row.getStatus());
        return toDto(batch, true);
    }

    @Override
    @Transactional
    public ImportBatchDto confirm(Long batchId) {
        ImportBatch batch = getPending(batchId);
        if (batch.getRows().stream().anyMatch(row -> row.getStatus() == ImportRowStatus.UNRESOLVED)) {
            throw new BusinessRuleException(UNRESOLVED_LEFT);
        }
        Stage stage = batch.getStage();
        int created = 0;
        int enrolled = 0;
        int tutors = 0;
        int updated = 0;
        for (ImportRow row : batch.getRows()) {
            if (row.getStatus() == ImportRowStatus.IGNORED) {
                continue;
            }
            StudentRowPayload payload = payload(row);
            Long studentId;
            if (row.getStatus() == ImportRowStatus.NEW_STUDENT) {
                // the same rules as registering by hand (a DNI taken meanwhile rejects the batch)
                studentId = studentService.create(toRequest(payload)).id();
                row.setMatchedStudent(studentRepository.getReferenceById(studentId));
                created++;
            } else {
                studentId = row.getMatchedStudent().getId();
                updateContactData(row.getMatchedStudent(), payload);
                updated++;
            }
            if (stage == null) {
                continue; // only registering
            }
            Long careerId = activeCareer(payload.careerId()) ? payload.careerId() : null;
            Long tutorId = payload.tutorId() != null && isActiveTutor(payload.tutorId()) ? payload.tutorId() : null;
            Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndStageId(studentId, stage.getId());
            Long enrollmentId;
            if (existing.isPresent()) {
                // already enrolled: never twice. A withdrawn enrollment is history and stays as it is;
                // an active one takes the career the file found (an empty or unknown one changes nothing)
                Enrollment enrollment = existing.get();
                if (!enrollment.isActive()) {
                    continue;
                }
                if (careerId != null) {
                    enrollment.setCareer(careerRepository.getReferenceById(careerId));
                }
                enrollmentId = enrollment.getId();
            } else {
                // the same rules as enrolling by hand: an inactive student or a closed cycle reject the batch
                enrollmentId = enrollmentService.create(new CreateEnrollmentRequest(studentId, stage.getId(), careerId)).id();
                enrolled++;
            }
            if (tutorId != null && tutorAssignmentService.assign(enrollmentId, tutorId).created()) {
                tutors++; // the same tutor again changes nothing; another one keeps the history
            }
        }
        batch.setStatus(ImportBatchStatus.CONFIRMED);
        batch.setConfirmedBy(currentUser.id());
        batch.setConfirmedAt(OffsetDateTime.now(clock));
        log.info("Student import confirmed: batchId={} created={} updated={} enrolled={} tutorsAssigned={} rows={}",
                batchId, created, updated, enrolled, tutors, batch.getRows().size());
        return toDto(batch, true);
    }

    /**
     * A registered student takes the contact data the file brings: email,
     * phone and birth date, and the DNI if they had none. An empty cell
     * erases nothing, and names are never overwritten (a mistyped DNI would
     * rename someone else): those are fixed in the student's file. Whoever
     * confirms the batch may do this, also a tutor who is not theirs. An
     * inactive student is not edited until reactivated.
     */
    private void updateContactData(Student student, StudentRowPayload payload) {
        if (!student.isActive()) {
            return;
        }
        if (payload.email() != null) {
            student.setEmail(payload.email());
        }
        if (payload.phone() != null) {
            student.setPhone(payload.phone());
        }
        if (payload.birthDate() != null) {
            student.setBirthDate(LocalDate.parse(payload.birthDate()));
        }
        if (student.getDni() == null && payload.dni() != null && !studentRepository.existsByDni(payload.dni())) {
            student.setDni(payload.dni());
        }
    }

    @Override
    @Transactional
    public void discard(Long batchId) {
        ImportBatch batch = getPending(batchId);
        batch.setStatus(ImportBatchStatus.DISCARDED);
        log.info("Student import discarded: batchId={}", batchId);
    }

    /**
     * Decides what each row is when the file is uploaded. It loads what the
     * matching needs once per file: registered students, active careers and
     * active tutors.
     */
    private final class Matcher {

        private final Stage stage;
        private final List<Student> students = studentRepository.findAll();
        private final Map<String, Student> byDni = students.stream().filter(s -> s.getDni() != null)
                .collect(Collectors.toMap(Student::getDni, Function.identity(), (a, b) -> a));
        private final List<Career> careers = careerRepository.findAll().stream().filter(Career::isActive).toList();
        private final List<User> tutors = userRepository.findByActiveTrue().stream()
                .filter(ImportNames::isTutor).toList();

        Matcher(Stage stage) {
            this.stage = stage;
        }

        ImportRow toRow(ImportBatch batch, StudentSheet.ParsedRow parsed) {
            UpsertStudentRequest s = parsed.student();
            Long careerId = matchCareer(parsed.careerText());
            Long tutorId = matchTutor(parsed.tutorText());
            ImportRow row = new ImportRow();
            row.setBatch(batch);
            row.setRowNumber(parsed.rowNumber());

            String notice = null;
            // sure it is the same person: the DNI matches or, while the row
            // or the student has no DNI to contradict it, the full name is
            // exactly that of one single student
            Student same = s.dni() == null ? null : byDni.get(s.dni());
            if (same == null) {
                Student named = onlyOneNamed(s);
                if (named != null && (s.dni() == null || named.getDni() == null)) {
                    same = named;
                }
            }
            if (same != null) {
                row.setMatchedStudent(same);
                row.setStatus(ImportRowStatus.AUTO_MATCHED);
                if (stage != null && !same.isActive()) {
                    // can't be enrolled: a person reactivates them or ignores the row
                    notice = INACTIVE_NOTICE;
                    row.setStatus(ImportRowStatus.UNRESOLVED);
                } else if (stage != null && enrollmentRepository.existsByStudentIdAndStageId(same.getId(), stage.getId())) {
                    notice = ALREADY_ENROLLED;
                }
            } else {
                List<Long> candidates = candidates(s);
                row.setCandidateMatches(json(candidates));
                row.setStatus(candidates.isEmpty() ? ImportRowStatus.NEW_STUDENT : ImportRowStatus.UNRESOLVED);
            }
            row.setRawPayload(json(new StudentRowPayload(s.firstName(), s.lastNamePat(), s.lastNameMat(), s.dni(),
                    s.email(), s.phone(), s.birthDate() == null ? null : s.birthDate().toString(),
                    parsed.careerText(), careerId, parsed.tutorText(), tutorId, notice)));
            return row;
        }

        private String nameOf(Student student) {
            return normalize(fullName(student.getLastNamePat(), student.getLastNameMat(), student.getFirstName()));
        }

        /** The one student with exactly this full name (ignoring case and accents); null if none or several. */
        private Student onlyOneNamed(UpsertStudentRequest s) {
            String name = normalize(fullName(s.lastNamePat(), s.lastNameMat(), s.firstName()));
            List<Student> found = students.stream().filter(student -> nameOf(student).equals(name)).toList();
            return found.size() == 1 ? found.get(0) : null;
        }

        /**
         * Registered students a person must compare the row with: a very
         * similar name, or (when both have a DNI and they differ) the very
         * same name, which smells like a mistyped DNI rather than a new
         * student. A similar name with another DNI is someone else.
         */
        private List<Long> candidates(UpsertStudentRequest s) {
            String name = normalize(fullName(s.lastNamePat(), s.lastNameMat(), s.firstName()));
            return students.stream()
                    .map(student -> Map.entry(student, similarity.apply(name, nameOf(student))))
                    .filter(entry -> s.dni() == null || entry.getKey().getDni() == null || entry.getValue() >= 1.0)
                    .filter(entry -> entry.getValue() >= SIMILAR_NAME)
                    .sorted(Map.Entry.<Student, Double>comparingByValue().reversed())
                    .limit(MAX_CANDIDATES)
                    .map(entry -> entry.getKey().getId())
                    .toList();
        }

        private Long matchCareer(String text) {
            if (text == null) {
                return null;
            }
            String wanted = normalize(text);
            List<Career> found = careers.stream().filter(c -> normalize(c.getName()).equals(wanted)).toList();
            return found.size() == 1 ? found.get(0).getId() : null;
        }

        private Long matchTutor(String text) {
            return ImportNames.matchTutor(tutors, text);
        }
    }

    private ImportBatchDto toDto(ImportBatch batch, boolean withRows) {
        List<ImportRow> rows = batch.getRows();
        Map<ImportRowStatus, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(ImportRow::getStatus, Collectors.counting()));
        int matched = count(counts, ImportRowStatus.AUTO_MATCHED) + count(counts, ImportRowStatus.MANUAL_MATCHED);
        Stage stage = batch.getStage();
        String stageLabel = stage == null ? null : stage.getCycle().getName() + " · " + stage.getName();
        return new ImportBatchDto(batch.getId(), batch.getType(), batch.getStatus(), batch.getFileName(),
                stage == null ? null : stage.getId(), stageLabel, null, null, batch.getUploadedAt(), rows.size(),
                count(counts, ImportRowStatus.NEW_STUDENT), matched, count(counts, ImportRowStatus.UNRESOLVED),
                count(counts, ImportRowStatus.IGNORED), 0, false, withRows ? rowsDto(rows) : List.of());
    }

    private List<ImportRowDto> rowsDto(List<ImportRow> rows) {
        Map<Long, String> careerNames = careerRepository.findAll().stream()
                .collect(Collectors.toMap(Career::getId, Career::getName));
        Map<Long, User> users = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> candidateIds = rows.stream().flatMap(row -> candidateIds(row).stream()).distinct().toList();
        Map<Long, Student> candidates = studentRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        return rows.stream().sorted(Comparator.comparingInt(ImportRow::getRowNumber)).map(row -> {
            StudentRowPayload p = payload(row);
            User tutor = p.tutorId() == null ? null : users.get(p.tutorId());
            return new ImportRowDto(row.getId(), row.getRowNumber(), row.getStatus(),
                    displayName(p.lastNamePat(), p.lastNameMat(), p.firstName()), p.dni(), p.email(), p.phone(),
                    p.birthDate(), p.careerText(), p.careerId() == null ? null : careerNames.get(p.careerId()),
                    p.tutorText(), tutor == null ? null : displayName(tutor.getLastNamePat(), tutor.getLastNameMat(), tutor.getFirstName()),
                    p.notice(), ImportNames.ref(row.getMatchedStudent()),
                    candidateIds(row).stream().map(candidates::get).filter(Objects::nonNull).map(ImportNames::ref).toList(),
                    false, null, null);
        }).toList();
    }

    private static UpsertStudentRequest toRequest(StudentRowPayload p) {
        return new UpsertStudentRequest(p.firstName(), p.lastNamePat(), p.lastNameMat(), p.dni(), p.email(), p.phone(),
                p.birthDate() == null ? null : LocalDate.parse(p.birthDate()));
    }

    private boolean activeCareer(Long careerId) {
        return careerId != null && careerRepository.findById(careerId).map(Career::isActive).orElse(false);
    }

    private boolean isActiveTutor(Long userId) {
        return userRepository.findById(userId).filter(User::isActive).filter(ImportNames::isTutor).isPresent();
    }

    private ImportBatch getOrThrow(Long id) {
        return batchRepository.findById(id)
                .filter(batch -> batch.getType() == ImportBatchType.STUDENTS)
                .orElseThrow(() -> new ResourceNotFoundException(BATCH_NOT_FOUND));
    }

    private ImportBatch getPending(Long id) {
        ImportBatch batch = getOrThrow(id);
        if (batch.getStatus() != ImportBatchStatus.PENDING_REVIEW) {
            throw new BusinessRuleException(NOT_PENDING);
        }
        return batch;
    }

    private StudentRowPayload payload(ImportRow row) {
        return jsonMapper.readValue(row.getRawPayload(), StudentRowPayload.class);
    }

    private List<Long> candidateIds(ImportRow row) {
        return row.getCandidateMatches() == null ? List.of()
                : jsonMapper.readValue(row.getCandidateMatches(), new TypeReference<List<Long>>() { });
    }

    private String json(Object value) {
        return jsonMapper.writeValueAsString(value);
    }

    private static int count(Map<ImportRowStatus, Long> counts, ImportRowStatus status) {
        return counts.getOrDefault(status, 0L).intValue();
    }

}
