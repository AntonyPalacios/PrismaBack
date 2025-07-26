package pe.com.edu.prismaapp.prisma.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.services.StudentService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TUTOR')")
    ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO c = studentService.save(studentDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or " +
                  "(hasRole('TUTOR') and @studentServiceImpl.isStudentAssignedToTutor(#id,authentication.principal.id))")
    ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id,
                                             @Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO c = studentService.update(id, studentDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Object> deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    ResponseEntity<List<StudentDTO>> getAllStudentsByStage(@RequestParam(name = "stageId", required = false) Optional<Long> stageId) {
        List<StudentDTO> students = studentService.findAll(stageId);
        return ResponseEntity.status(HttpStatus.OK).body(students);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path="/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    ResponseEntity<Object> uploadStudents(@RequestParam("file") MultipartFile file) {
        studentService.uploadStudents(file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
