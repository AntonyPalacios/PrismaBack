package pe.com.edu.prismaapp.prisma.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.services.StudentService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    private ResourceLoader resourceLoader;

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

    @GetMapping("/template")
    ResponseEntity<Resource> getStudentTemplate() throws IOException {
        Resource resource = new ClassPathResource("templates/Plantilla Importar Alumnos.xlsx");
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .contentLength(resource.contentLength())
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .headers(headers)
                .body(resource);
    }
}
