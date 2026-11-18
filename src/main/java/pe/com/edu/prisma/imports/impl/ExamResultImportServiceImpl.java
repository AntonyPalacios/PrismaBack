package pe.com.edu.prisma.imports.impl;

import static pe.com.edu.prisma.imports.impl.ImportNames.normalize;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prisma.academic.CycleRepository;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.StageService;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.AreaRepository;
import pe.com.edu.prisma.catalog.Career;
import pe.com.edu.prisma.catalog.CareerRepository;
import pe.com.edu.prisma.catalog.CourseRepository;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.catalog.ExamTypeRepository;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ConfirmationRequiredException;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.ExamAreaResult;
import pe.com.edu.prisma.exams.ExamCourseResult;
import pe.com.edu.prisma.exams.ExamRepository;
import pe.com.edu.prisma.exams.ExamResult;
import pe.com.edu.prisma.exams.ExamResultRepository;
import pe.com.edu.prisma.imports.ExamResultImportService;
import pe.com.edu.prisma.imports.ImportBatch;
import pe.com.edu.prisma.imports.ImportBatchRepository;
import pe.com.edu.prisma.imports.ImportBatchStatus;
import pe.com.edu.prisma.imports.ImportBatchType;
import pe.com.edu.prisma.imports.ImportRow;
import pe.com.edu.prisma.imports.ImportRowRepository;
import pe.com.edu.prisma.imports.ImportRowStatus;
import pe.com.edu.prisma.imports.ResultRowPayload;
import pe.com.edu.prisma.imports.ResultRowPayload.NewStudent;
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
import pe.com.edu.prisma.tutoring.TutorAssignment;
import pe.com.edu.prisma.tutoring.TutorAssignmentRepository;
import pe.com.edu.prisma.tutoring.TutorAssignmentService;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Importing an exam's results (REQ-EXAMS-02, agreements of 2026-11-16): the
 * academy's Excel becomes a review batch; on confirm, each row's student
 * (found, chosen or registered by hand) is enrolled in the exam's stage if
 * they were not (in the career named as the exam type), and their result
 * is saved. Whoever is left without a tutor in the stage gets the one the
 * file's TUTOR column names; a tutor a student already has is never replaced.
 */
@Service
@Transactional(readOnly = true)
public class ExamResultImportServiceImpl implements ExamResultImportService {

    static final String EXAM_NOT_FOUND = "No encontramos el examen solicitado.";
    static final String NOT_CURRENT_STAGE = "Solo se cargan resultados de los exámenes de la etapa vigente del ciclo activo.";
    static final String HAS_RESULTS = "Este examen ya tiene resultados: no se pueden cargar otra vez.";
    static final String ONLY_ADMIN_RELOADS = "Solo un administrador puede recargar los resultados de un examen.";
    static final String EXAM_GONE = "El examen de esta importación ya no existe: descártala.";
    static final String CHOOSE_STUDENT = "Elige el alumno con el que se empareja la fila.";
    static final String WITHDRAWN = "Está retirado de esta etapa: no se le pueden cargar resultados. Ignora la fila.";
    static final String INACTIVE = "Está dado de baja y no está matriculado en la etapa: reactívalo antes de confirmar o ignora la fila.";
    static final String WILL_ENROLL = "No está matriculado en la etapa: se matriculará al confirmar en %s.";
    static final String WILL_ENROLL_NO_CAREER = "No está matriculado en la etapa: se matriculará al confirmar, sin carrera.";
    static final String TAKEN_BY_ROW = "Ese alumno ya está en la fila %d: ignora una de las dos.";
    static final String SAME_STUDENT = "Las filas %d y %d son el mismo alumno: ignora una de las dos.";
    static final String PENDING_EXISTS = "Este examen ya tiene una importación en revisión: continúala o descártala antes de subir otra.";
    static final String DNI_REPEATED_IN_FILE = "El DNI del archivo se repite en otras filas: se ignoró.";
    static final String DNI_NOT_MATCHING = "El DNI del archivo no coincide con el registrado: se mantiene el registrado.";
    static final String NEW_WITHOUT_DNI = "Ese DNI ya es de otro alumno: se registrará sin DNI.";
    static final String OTHER_TYPE = "Ya tiene resultado en %s de este simulacro: ignora la fila o reemplaza ese resultado.";
    static final String REPLACES_OTHER = "Al confirmar se borrará su resultado de %s de este simulacro.";
    static final String OTHER_TYPE_ROW = "Fila %d: ya tiene resultado en %s de este simulacro.";

    // how alike two full names must be to suggest "this may be the same person"
    private static final double SIMILAR_NAME = 0.92;
    private static final int MAX_CANDIDATES = 5;

    private static final Logger log = LoggerFactory.getLogger(ExamResultImportServiceImpl.class);

    private final ImportBatchRepository batchRepository;
    private final ImportRowRepository rowRepository;
    private final ResultSheet sheet;
    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final ExamTypeRepository examTypeRepository;
    private final CourseRepository courseRepository;
    private final AreaRepository areaRepository;
    private final CareerRepository careerRepository;
    private final CycleRepository cycleRepository;
    private final StageService stageService;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final UserRepository userRepository;
    private final TutorAssignmentRepository tutorAssignmentRepository;
    private final TutorAssignmentService tutorAssignmentService;
    private final CurrentUser currentUser;
    private final Validator validator;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public ExamResultImportServiceImpl(ImportBatchRepository batchRepository, ImportRowRepository rowRepository,
            ResultSheet sheet, ExamRepository examRepository, ExamResultRepository resultRepository,
            ExamTypeRepository examTypeRepository, CourseRepository courseRepository, AreaRepository areaRepository,
            CareerRepository careerRepository,
            CycleRepository cycleRepository, StageService stageService, StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository, StudentService studentService,
            EnrollmentService enrollmentService, UserRepository userRepository,
            TutorAssignmentRepository tutorAssignmentRepository, TutorAssignmentService tutorAssignmentService,
            CurrentUser currentUser, Validator validator, JsonMapper jsonMapper, Clock clock) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.sheet = sheet;
        this.examRepository = examRepository;
        this.resultRepository = resultRepository;
        this.examTypeRepository = examTypeRepository;
        this.courseRepository = courseRepository;
        this.areaRepository = areaRepository;
        this.careerRepository = careerRepository;
        this.cycleRepository = cycleRepository;
        this.stageService = stageService;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
        this.userRepository = userRepository;
        this.tutorAssignmentRepository = tutorAssignmentRepository;
        this.tutorAssignmentService = tutorAssignmentService;
        this.currentUser = currentUser;
        this.validator = validator;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ImportBatchDto upload(Long examId, MultipartFile file, boolean reload, boolean replacePending) {
        if (reload && !currentUser.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException(ONLY_ADMIN_RELOADS);
        }
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ResourceNotFoundException(EXAM_NOT_FOUND));
        Stage stage = exam.getMockExam().getStage();
        boolean current = cycleRepository.findFirstByStatus(CycleStatus.ACTIVE)
                .flatMap(cycle -> stageService.findCurrent(cycle.getId()))
                .filter(currentStage -> currentStage.getId().equals(stage.getId()))
                .isPresent();
        if (!current) {
            throw new BusinessRuleException(NOT_CURRENT_STAGE);
        }
        boolean hasResults = resultRepository.existsByExamId(examId);
        if (hasResults && !reload) {
            throw new BusinessRuleException(HAS_RESULTS);
        }
        // one import in review per exam: the user continues it, or discards it to upload this one
        Optional<ImportBatch> pending = batchRepository.findFirstByExamIdAndStatusOrderByIdDesc(examId,
                ImportBatchStatus.PENDING_REVIEW);
        if (pending.isPresent() && !replacePending) {
            ImportBatch other = pending.get();
            String uploader = userRepository.findById(other.getUploadedBy())
                    .map(u -> ImportNames.displayName(u.getLastNamePat(), u.getLastNameMat(), u.getFirstName()))
                    .orElse("");
            throw new ConfirmationRequiredException(PENDING_EXISTS, Map.of(
                    "pendingBatchId", other.getId(),
                    "pendingFileName", other.getFileName(),
                    "pendingUploadedAt", other.getUploadedAt().toString(),
                    "pendingUploadedBy", uploader));
        }
        pending.ifPresent(other -> {
            other.setStatus(ImportBatchStatus.DISCARDED);
            log.info("Exam results import discarded to upload another: batchId={} examId={}", other.getId(), examId);
        });
        ExamType type = exam.getExamType();
        List<String> otherTypes = examTypeRepository.findAll().stream()
                .filter(other -> !other.getId().equals(type.getId()))
                .map(ExamType::getName)
                .toList();
        ResultSheet.Parsed parsed;
        try (InputStream in = file.getInputStream()) {
            parsed = sheet.read(in, type, otherTypes);
        } catch (IOException e) {
            throw new InvalidRequestException("No pudimos leer el archivo: vuelve a subirlo.");
        }

        ImportBatch batch = new ImportBatch();
        batch.setType(ImportBatchType.EXAM_RESULTS);
        batch.setExam(exam);
        batch.setStage(stage);
        batch.setSkippedRows(parsed.skipped());
        batch.setReplacesResults(hasResults); // a reload of an exam without results is a plain load
        batch.setFileName(Optional.ofNullable(file.getOriginalFilename()).orElse("resultados.xlsx"));
        batch.setUploadedBy(currentUser.id());
        batch.setUploadedAt(OffsetDateTime.now(clock));
        batchRepository.save(batch);

        Matcher matcher = new Matcher(exam, defaultCareer(type).orElse(null));
        for (ResultSheet.ParsedRow parsedRow : parsed.rows()) {
            batch.getRows().add(matcher.toRow(batch, parsedRow));
        }
        rowRepository.saveAll(batch.getRows());
        log.info("Exam results import uploaded: batchId={} examId={} rows={} skipped={}", batch.getId(), examId,
                parsed.rows().size(), parsed.skipped());
        return toDto(batch, true);
    }

    @Override
    public List<ImportBatchDto> findAll(ImportBatchStatus status) {
        List<ImportBatch> batches = status == null
                ? batchRepository.findByTypeOrderByUploadedAtDescIdDesc(ImportBatchType.EXAM_RESULTS)
                : batchRepository.findByTypeAndStatusOrderByUploadedAtDescIdDesc(ImportBatchType.EXAM_RESULTS, status);
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
                .orElseThrow(() -> new ResourceNotFoundException(StudentImportServiceImpl.ROW_NOT_FOUND));
        ResultRowPayload payload = payload(row);
        switch (request.action()) {
            case MATCH -> {
                if (request.studentId() == null) {
                    throw new InvalidRequestException(CHOOSE_STUDENT);
                }
                Student student = studentRepository.findById(request.studentId())
                        .orElseThrow(() -> new ResourceNotFoundException(StudentImportServiceImpl.STUDENT_NOT_FOUND));
                rejectTaken(batch, row, student.getId());
                Career career = defaultCareer(batch.getExam().getExamType()).orElse(null);
                Standing standing = standing(student, batch.getStage(), career);
                if (standing.blocker() != null) {
                    throw new BusinessRuleException(standing.blocker());
                }
                // a student sits one type per mock exam: replacing the other result is a person's decision
                Optional<ExamResult> other = otherTypeResult(batch.getExam(), student.getId());
                boolean replace = Boolean.TRUE.equals(request.replaceOther());
                if (other.isPresent() && !replace) {
                    throw new BusinessRuleException(OTHER_TYPE.formatted(other.get().getExam().getExamType().getName()));
                }
                row.setMatchedStudent(student);
                row.setStatus(ImportRowStatus.MANUAL_MATCHED);
                ResultRowPayload reviewed = payload.withReview(standing.enrolls(), null, other
                        .map(o -> REPLACES_OTHER.formatted(o.getExam().getExamType().getName()))
                        .orElse(standing.notice()));
                row.setRawPayload(json(other
                        .map(o -> reviewed.withOther(o.getExam().getId(), o.getExam().getExamType().getName(), true))
                        .orElse(reviewed)));
            }
            case NEW -> {
                NewStudent names = new NewStudent(request.firstName(), request.lastNamePat(), request.lastNameMat());
                // the file's DNI is often someone else's: the new student is registered without it
                boolean dniTaken = payload.dni() != null && studentRepository.existsByDni(payload.dni());
                ResultRowPayload base = dniTaken ? payload.withoutDni() : payload;
                UpsertStudentRequest student = toRequest(names, base.dni());
                List<String> errors = validator.validate(student).stream()
                        .map(ConstraintViolation::getMessage)
                        .sorted()
                        .toList();
                if (!errors.isEmpty()) {
                    throw new InvalidRequestException(String.join(" ", errors));
                }
                row.setMatchedStudent(null);
                row.setStatus(ImportRowStatus.NEW_STUDENT);
                row.setRawPayload(json(base.withReview(true, new NewStudent(student.firstName(),
                        student.lastNamePat(), student.lastNameMat()), dniTaken ? NEW_WITHOUT_DNI : null)));
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
            throw new BusinessRuleException(StudentImportServiceImpl.UNRESOLVED_LEFT);
        }
        Exam exam = examRepository.findById(batch.getExam().getId())
                .orElseThrow(() -> new BusinessRuleException(EXAM_GONE));
        if (batch.isReplacesResults() && !currentUser.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException(ONLY_ADMIN_RELOADS);
        }
        if (!batch.isReplacesResults() && resultRepository.existsByExamId(exam.getId())) {
            throw new BusinessRuleException(HAS_RESULTS);
        }
        List<ImportRow> rows = batch.getRows().stream()
                .filter(row -> row.getStatus() != ImportRowStatus.IGNORED)
                .toList();
        rejectSameStudentTwice(rows);

        // a student sits one type per mock exam: whoever got a result in another type meanwhile stops the batch,
        // unless a person chose to replace it
        Map<Long, ExamResult> otherResults = resultRepository
                .findInOtherExamsOfMockExam(exam.getMockExam().getId(), exam.getId()).stream()
                .collect(Collectors.toMap(r -> r.getEnrollment().getStudent().getId(), Function.identity(), (a, b) -> a));
        int replacedOther = 0;
        for (ImportRow row : rows) {
            if (row.getMatchedStudent() == null) {
                continue;
            }
            ExamResult other = otherResults.get(row.getMatchedStudent().getId());
            if (other == null) {
                continue;
            }
            if (!payload(row).replacesOther()) {
                throw new BusinessRuleException(OTHER_TYPE_ROW.formatted(row.getRowNumber(),
                        other.getExam().getExamType().getName()));
            }
            resultRepository.delete(other);
            replacedOther++;
        }
        if (replacedOther > 0) {
            resultRepository.flush();
        }

        int replaced = 0;
        if (batch.isReplacesResults()) {
            // the forced reload: out with the exam's results, in the same transaction as the new ones
            List<ExamResult> previous = resultRepository.findByExamId(exam.getId());
            replaced = previous.size();
            resultRepository.deleteAll(previous);
            resultRepository.flush(); // before inserting: one result per student and exam
        }

        Stage stage = batch.getStage();
        Long careerId = defaultCareer(exam.getExamType()).map(Career::getId).orElse(null);
        int created = 0;
        int enrolled = 0;
        int tutors = 0;
        for (ImportRow row : rows) {
            ResultRowPayload payload = payload(row);
            Long studentId;
            if (row.getStatus() == ImportRowStatus.NEW_STUDENT) {
                // the same rules as registering by hand (a DNI taken meanwhile rejects the batch)
                studentId = studentService.create(toRequest(payload.newStudent(), payload.dni())).id();
                row.setMatchedStudent(studentRepository.getReferenceById(studentId));
                created++;
            } else {
                studentId = row.getMatchedStudent().getId();
            }
            Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndStageId(studentId, stage.getId());
            Enrollment enrollment;
            if (existing.isPresent()) {
                if (!existing.get().isActive()) {
                    throw new BusinessRuleException("Fila " + row.getRowNumber() + ": " + WITHDRAWN);
                }
                enrollment = existing.get();
            } else {
                // the same rules as enrolling by hand: an inactive student or a closed cycle reject the batch
                Long enrollmentId = enrollmentService.create(new CreateEnrollmentRequest(studentId, stage.getId(), careerId)).id();
                enrollment = enrollmentRepository.getReferenceById(enrollmentId);
                enrolled++;
            }
            // the file's tutor only for who has none in the stage: one they already have is never replaced
            if (payload.tutorId() != null && isActiveTutor(payload.tutorId())
                    && tutorAssignmentRepository.findByEnrollmentIdAndValidToIsNull(enrollment.getId()).isEmpty()) {
                tutorAssignmentService.assign(enrollment.getId(), payload.tutorId());
                tutors++;
            }
            resultRepository.save(toResult(exam, enrollment, payload));
        }
        batch.setStatus(ImportBatchStatus.CONFIRMED);
        batch.setConfirmedBy(currentUser.id());
        batch.setConfirmedAt(OffsetDateTime.now(clock));
        log.info("Exam results import confirmed: batchId={} examId={} results={} studentsCreated={} enrolled={} "
                + "tutorsAssigned={} replaced={} replacedOtherTypes={}", batchId, exam.getId(), rows.size(), created, enrolled,
                tutors, replaced, replacedOther);
        return toDto(batch, true);
    }

    @Override
    @Transactional
    public void discard(Long batchId) {
        ImportBatch batch = getPending(batchId);
        batch.setStatus(ImportBatchStatus.DISCARDED);
        log.info("Exam results import discarded: batchId={}", batchId);
    }

    private ExamResult toResult(Exam exam, Enrollment enrollment, ResultRowPayload payload) {
        ExamResult result = new ExamResult();
        result.setExam(exam);
        result.setEnrollment(enrollment);
        result.setAttended(payload.attended());
        result.setTotalCorrect(payload.totalCorrect());
        result.setTotalWrong(payload.totalWrong());
        result.setTotalBlank(payload.totalBlank());
        result.setTotalScore(payload.totalScore());
        for (ResultRowPayload.CourseCount count : payload.courses()) {
            ExamCourseResult course = new ExamCourseResult();
            course.setExamResult(result);
            course.setCourse(courseRepository.getReferenceById(count.courseId()));
            course.setCorrect(count.correct());
            course.setWrong(count.wrong());
            course.setBlank(count.blank());
            result.getCourses().add(course);
        }
        for (ResultRowPayload.AreaScore score : payload.areas()) {
            ExamAreaResult area = new ExamAreaResult();
            area.setExamResult(result);
            area.setArea(areaRepository.getReferenceById(score.areaId()));
            area.setScore(score.score());
            result.getAreas().add(area);
        }
        return result;
    }

    /** Where a student stands in the stage: what blocks the row, or whether confirming enrolls them. */
    private record Standing(boolean enrolls, String notice, String blocker) {
    }

    private Standing standing(Student student, Stage stage, Career career) {
        Optional<Enrollment> enrollment = enrollmentRepository.findByStudentIdAndStageId(student.getId(), stage.getId());
        if (enrollment.isPresent()) {
            return enrollment.get().isActive() ? new Standing(false, null, null) : new Standing(false, null, WITHDRAWN);
        }
        String notice = career == null ? WILL_ENROLL_NO_CAREER : WILL_ENROLL.formatted(career.getName());
        return student.isActive() ? new Standing(true, notice, null) : new Standing(false, null, INACTIVE);
    }

    /**
     * The career whoever gets enrolled by the import takes: the active one of
     * the exam's type named as the type (Ciencias, Letras, Arquitectura). A
     * Derecho or Biomédica student is corrected later in their enrollment.
     */
    private Optional<Career> defaultCareer(ExamType type) {
        String wanted = normalize(type.getName());
        return careerRepository.findAll().stream()
                .filter(Career::isActive)
                .filter(career -> career.getExamType().getId().equals(type.getId()))
                .filter(career -> normalize(career.getName()).equals(wanted))
                .findFirst();
    }

    private boolean isActiveTutor(Long userId) {
        return userRepository.findById(userId).filter(User::isActive).filter(ImportNames::isTutor).isPresent();
    }

    /** The student's result in another type (exam) of the same mock exam, if any. */
    private Optional<ExamResult> otherTypeResult(Exam exam, Long studentId) {
        return resultRepository.findInOtherExamsOfMockExam(exam.getMockExam().getId(), exam.getId()).stream()
                .filter(r -> r.getEnrollment().getStudent().getId().equals(studentId))
                .findFirst();
    }

    // a student can't take two rows of the same exam
    private void rejectTaken(ImportBatch batch, ImportRow row, Long studentId) {
        batch.getRows().stream()
                .filter(other -> !other.getId().equals(row.getId()))
                .filter(other -> other.getStatus() != ImportRowStatus.IGNORED)
                .filter(other -> other.getMatchedStudent() != null && other.getMatchedStudent().getId().equals(studentId))
                .findFirst()
                .ifPresent(other -> {
                    throw new BusinessRuleException(TAKEN_BY_ROW.formatted(other.getRowNumber()));
                });
    }

    private static void rejectSameStudentTwice(List<ImportRow> rows) {
        Map<Long, Integer> seen = new HashMap<>();
        for (ImportRow row : rows) {
            if (row.getMatchedStudent() == null) {
                continue;
            }
            Integer first = seen.putIfAbsent(row.getMatchedStudent().getId(), row.getRowNumber());
            if (first != null) {
                throw new BusinessRuleException(SAME_STUDENT.formatted(first, row.getRowNumber()));
            }
        }
    }

    /**
     * Decides what each row is when the file is uploaded (agreements of
     * 2026-11-18): the name decides, the DNI only confirms it, because the
     * academy's files often carry someone else's DNI. The same student
     * without review when the DNI matches and the name is similar, or when
     * the name is exactly that of one single student (whatever the DNI says).
     * A DNI whose owner has another name is not trusted. Anyone else waits
     * for a person, with the students it may be. A student who already has a
     * result in another type of the mock exam always waits for a person.
     */
    private final class Matcher {

        private final Exam exam;
        private final Stage stage;
        private final Career career;
        private final List<Student> students = studentRepository.findAll();
        private final Map<String, Student> byDni = students.stream().filter(s -> s.getDni() != null)
                .collect(Collectors.toMap(Student::getDni, Function.identity(), (a, b) -> a));
        private final Map<Long, String> names = students.stream()
                .collect(Collectors.toMap(Student::getId, ImportNames::comparableName));
        private final List<User> tutors = userRepository.findByActiveTrue().stream().filter(ImportNames::isTutor).toList();
        private final Map<Long, ExamResult> otherTypes;

        Matcher(Exam exam, Career career) {
            this.exam = exam;
            this.stage = exam.getMockExam().getStage();
            this.career = career;
            this.otherTypes = resultRepository.findInOtherExamsOfMockExam(exam.getMockExam().getId(), exam.getId()).stream()
                    .collect(Collectors.toMap(r -> r.getEnrollment().getStudent().getId(), Function.identity(), (a, b) -> a));
        }

        ImportRow toRow(ImportBatch batch, ResultSheet.ParsedRow parsed) {
            ImportRow row = new ImportRow();
            row.setBatch(batch);
            row.setRowNumber(parsed.rowNumber());
            String name = normalize(parsed.fullName());
            List<String> notes = new ArrayList<>();
            if (parsed.dniIgnored()) {
                notes.add(DNI_REPEATED_IN_FILE);
            }

            // the DNI's owner only if the name agrees; else the one student with exactly this name
            Student owner = parsed.dni() == null ? null : byDni.get(parsed.dni());
            Student same = null;
            if (owner != null && similarity.apply(name, names.get(owner.getId())) >= SIMILAR_NAME) {
                same = owner;
            } else {
                List<Student> named = students.stream().filter(s -> names.get(s.getId()).equals(name)).toList();
                if (named.size() == 1) {
                    same = named.get(0);
                    if (parsed.dni() != null && !parsed.dni().equals(same.getDni())) {
                        notes.add(DNI_NOT_MATCHING);
                    }
                }
            }
            boolean enrolls = false;
            Long otherExamId = null;
            String otherTypeName = null;
            if (same != null) {
                Standing standing = standing(same, stage, career);
                ExamResult other = otherTypes.get(same.getId());
                row.setMatchedStudent(same);
                enrolls = standing.enrolls();
                if (standing.blocker() != null) {
                    row.setStatus(ImportRowStatus.UNRESOLVED);
                    notes.add(standing.blocker());
                } else if (other != null) {
                    row.setStatus(ImportRowStatus.UNRESOLVED);
                    otherExamId = other.getExam().getId();
                    otherTypeName = other.getExam().getExamType().getName();
                    notes.add(OTHER_TYPE.formatted(otherTypeName));
                } else {
                    row.setStatus(ImportRowStatus.AUTO_MATCHED);
                    if (standing.notice() != null) {
                        notes.add(standing.notice());
                    }
                }
            } else {
                row.setCandidateMatches(json(candidates(name, owner)));
                row.setStatus(ImportRowStatus.UNRESOLVED);
            }
            row.setRawPayload(json(new ResultRowPayload(parsed.fullName(), parsed.dni(), parsed.attended(),
                    parsed.courses(), parsed.areas(), parsed.totalScore(), parsed.tutorText(),
                    ImportNames.matchTutor(tutors, parsed.tutorText()), enrolls, null,
                    notes.isEmpty() ? null : String.join(" ", notes), otherExamId, otherTypeName, false)));
            return row;
        }

        /** The DNI's owner (when the name did not agree) first, then very similar names. */
        private List<Long> candidates(String name, Student dniOwner) {
            List<Long> ids = new ArrayList<>();
            if (dniOwner != null) {
                ids.add(dniOwner.getId());
            }
            students.stream()
                    .map(student -> Map.entry(student, similarity.apply(name, names.get(student.getId()))))
                    .filter(entry -> entry.getValue() >= SIMILAR_NAME)
                    .sorted(Map.Entry.<Student, Double>comparingByValue().reversed())
                    .map(entry -> entry.getKey().getId())
                    .filter(id -> !ids.contains(id))
                    .limit(MAX_CANDIDATES - ids.size())
                    .forEach(ids::add);
            return ids;
        }
    }

    private ImportBatchDto toDto(ImportBatch batch, boolean withRows) {
        List<ImportRow> rows = batch.getRows();
        Map<ImportRowStatus, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(ImportRow::getStatus, Collectors.counting()));
        int matched = count(counts, ImportRowStatus.AUTO_MATCHED) + count(counts, ImportRowStatus.MANUAL_MATCHED);
        Stage stage = batch.getStage();
        Exam exam = batch.getExam();
        // the exam may have been removed since: its label is then unknown
        String examLabel = examRepository.findById(exam.getId())
                .map(e -> e.getMockExam().getName() + " · " + e.getExamType().getName())
                .orElse(null);
        return new ImportBatchDto(batch.getId(), batch.getType(), batch.getStatus(), batch.getFileName(),
                stage.getId(), stage.getCycle().getName() + " · " + stage.getName(), exam.getId(), examLabel,
                batch.getUploadedAt(), rows.size(), count(counts, ImportRowStatus.NEW_STUDENT), matched,
                count(counts, ImportRowStatus.UNRESOLVED), count(counts, ImportRowStatus.IGNORED),
                batch.getSkippedRows(), batch.isReplacesResults(), withRows ? rowsDto(rows, stage) : List.of());
    }

    private List<ImportRowDto> rowsDto(List<ImportRow> rows, Stage stage) {
        Map<Long, String> areaNames = areaRepository.findAll().stream().collect(Collectors.toMap(Area::getId, Area::getName));
        Map<Long, User> users = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
        // the tutor each student already has in the stage: the file's tutor is only for who has none
        Map<Long, Long> enrollmentStudents = enrollmentRepository.findByStageIdAndActiveTrue(stage.getId()).stream()
                .collect(Collectors.toMap(Enrollment::getId, e -> e.getStudent().getId()));
        Map<Long, User> currentTutors = tutorAssignmentRepository
                .findByEnrollmentIdInAndValidToIsNull(enrollmentStudents.keySet()).stream()
                .collect(Collectors.toMap(a -> enrollmentStudents.get(a.getEnrollment().getId()), TutorAssignment::getTutor));
        List<Long> candidateIds = rows.stream().flatMap(row -> candidateIds(row).stream()).distinct().toList();
        Map<Long, Student> candidates = studentRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        return rows.stream().sorted(Comparator.comparingInt(ImportRow::getRowNumber)).map(row -> {
            ResultRowPayload p = payload(row);
            NewStudent n = p.newStudent();
            ImportRowDto.ResultDto result = new ImportRowDto.ResultDto(p.attended(), p.totalCorrect(), p.totalWrong(),
                    p.totalScore(), p.areas().stream()
                            .sorted(Comparator.comparing(ResultRowPayload.AreaScore::areaId))
                            .map(a -> new ImportRowDto.AreaScoreDto(areaNames.get(a.areaId()), a.score()))
                            .toList());
            // the tutor the student will have: the one they already have, else the one the file names
            User tutor = row.getMatchedStudent() == null ? null : currentTutors.get(row.getMatchedStudent().getId());
            if (tutor == null && p.tutorId() != null) {
                tutor = users.get(p.tutorId());
            }
            return new ImportRowDto(row.getId(), row.getRowNumber(), row.getStatus(), p.fullName(), p.dni(),
                    null, null, null, null, null, p.tutorText(),
                    tutor == null ? null : ImportNames.displayName(tutor.getLastNamePat(), tutor.getLastNameMat(), tutor.getFirstName()),
                    p.notice(), ImportNames.ref(row.getMatchedStudent()),
                    candidateIds(row).stream().map(candidates::get).filter(Objects::nonNull).map(ImportNames::ref).toList(),
                    p.enrolls() && row.getStatus() != ImportRowStatus.IGNORED,
                    n == null ? null : new ImportRowDto.NewStudentDto(n.firstName(), n.lastNamePat(), n.lastNameMat()),
                    result, p.otherExamTypeName(), p.replacesOther());
        }).toList();
    }

    private static UpsertStudentRequest toRequest(NewStudent names, String dni) {
        return new UpsertStudentRequest(trim(names.firstName()), trim(names.lastNamePat()), names.lastNameMat(), dni,
                null, null, null);
    }

    private static String trim(String text) {
        return text == null ? null : text.trim();
    }

    private ImportBatch getOrThrow(Long id) {
        return batchRepository.findById(id)
                .filter(batch -> batch.getType() == ImportBatchType.EXAM_RESULTS)
                .orElseThrow(() -> new ResourceNotFoundException(StudentImportServiceImpl.BATCH_NOT_FOUND));
    }

    private ImportBatch getPending(Long id) {
        ImportBatch batch = getOrThrow(id);
        if (batch.getStatus() != ImportBatchStatus.PENDING_REVIEW) {
            throw new BusinessRuleException(StudentImportServiceImpl.NOT_PENDING);
        }
        return batch;
    }

    private ResultRowPayload payload(ImportRow row) {
        return jsonMapper.readValue(row.getRawPayload(), ResultRowPayload.class);
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
