package pe.com.edu.prisma.students;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;
import pe.com.edu.prisma.students.dto.StudentDto;
import pe.com.edu.prisma.students.dto.UpsertStudentRequest;

/**
 * Students (REQ-STUDENTS-01): every role reads and searches them (a
 * coordinator needs to find students to work with exams); only ADMIN and
 * TUTOR register and edit. A tutor seeing "only my students" is a screen
 * default, not an access rule: other flows need any tutor to find any student.
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // what is typed in the search box is a name or a DNI: never logged
    @GetMapping
    public List<StudentDto> findAll(@Sensitive(LogMask.HIDDEN) @RequestParam(required = false) String search) {
        return studentService.findAll(search);
    }

    @GetMapping("/{id}")
    public StudentDto findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    public ResponseEntity<StudentDto> create(@Valid @RequestBody UpsertStudentRequest request) {
        StudentDto created = studentService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/students/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    public StudentDto update(@PathVariable Long id, @Valid @RequestBody UpsertStudentRequest request) {
        return studentService.update(id, request);
    }
}
