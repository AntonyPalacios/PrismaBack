package pe.com.edu.prisma.imports.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.imports.ExamResultImportService;
import pe.com.edu.prisma.imports.ImportBatchRepository;
import pe.com.edu.prisma.imports.ImportBatchService;
import pe.com.edu.prisma.imports.ImportBatchStatus;
import pe.com.edu.prisma.imports.ImportBatchType;
import pe.com.edu.prisma.imports.StudentImportService;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;
import pe.com.edu.prisma.imports.dto.ResolveRowRequest;
import pe.com.edu.prisma.imports.dto.StudentRefDto;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.users.Role;

@Service
@Transactional(readOnly = true)
public class ImportBatchServiceImpl implements ImportBatchService {

    static final String ONLY_STUDENT_IMPORTERS = "Solo un administrador o un tutor pueden ver las importaciones de alumnos.";
    private static final int MAX_SEARCH_RESULTS = 20;

    private final ImportBatchRepository batchRepository;
    private final StudentImportService studentImports;
    private final ExamResultImportService resultImports;
    private final StudentRepository studentRepository;
    private final CurrentUser currentUser;

    public ImportBatchServiceImpl(ImportBatchRepository batchRepository, StudentImportService studentImports,
            ExamResultImportService resultImports, StudentRepository studentRepository, CurrentUser currentUser) {
        this.batchRepository = batchRepository;
        this.studentImports = studentImports;
        this.resultImports = resultImports;
        this.studentRepository = studentRepository;
        this.currentUser = currentUser;
    }

    @Override
    public List<ImportBatchDto> findAll(ImportBatchStatus status) {
        Stream<ImportBatchDto> students = importsStudents() ? studentImports.findAll(status).stream() : Stream.empty();
        return Stream.concat(students, resultImports.findAll(status).stream())
                .sorted(Comparator.comparing(ImportBatchDto::uploadedAt).thenComparing(ImportBatchDto::id).reversed())
                .toList();
    }

    @Override
    public ImportBatchDto findById(Long id) {
        return typeOf(id) == ImportBatchType.STUDENTS ? studentImports.findById(id) : resultImports.findById(id);
    }

    @Override
    @Transactional
    public ImportBatchDto resolve(Long batchId, Long rowId, ResolveRowRequest request) {
        return typeOf(batchId) == ImportBatchType.STUDENTS
                ? studentImports.resolve(batchId, rowId, request)
                : resultImports.resolve(batchId, rowId, request);
    }

    @Override
    @Transactional
    public ImportBatchDto confirm(Long batchId) {
        return typeOf(batchId) == ImportBatchType.STUDENTS
                ? studentImports.confirm(batchId)
                : resultImports.confirm(batchId);
    }

    @Override
    @Transactional
    public void discard(Long batchId) {
        if (typeOf(batchId) == ImportBatchType.STUDENTS) {
            studentImports.discard(batchId);
        } else {
            resultImports.discard(batchId);
        }
    }

    @Override
    public List<StudentRefDto> searchStudents(Long batchId, String query) {
        typeOf(batchId);
        String wanted = ImportNames.normalize(query);
        if (wanted.length() < 2) {
            return List.of();
        }
        List<String> words = List.of(wanted.split(" "));
        return studentRepository.findAll().stream()
                .filter(student -> {
                    String haystack = ImportNames.normalize(ImportNames.fullName(student.getLastNamePat(),
                            student.getLastNameMat(), student.getFirstName(), student.getDni()));
                    return words.stream().allMatch(haystack::contains);
                })
                .map(ImportNames::ref)
                .sorted(Comparator.comparing(StudentRefDto::fullName, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_SEARCH_RESULTS)
                .toList();
    }

    // the batch's type; a students batch only for those who import students
    private ImportBatchType typeOf(Long id) {
        ImportBatchType type = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(StudentImportServiceImpl.BATCH_NOT_FOUND))
                .getType();
        if (type == ImportBatchType.STUDENTS && !importsStudents()) {
            throw new AccessDeniedException(ONLY_STUDENT_IMPORTERS);
        }
        return type;
    }

    private boolean importsStudents() {
        return currentUser.hasRole(Role.ADMIN) || currentUser.hasRole(Role.TUTOR);
    }
}
