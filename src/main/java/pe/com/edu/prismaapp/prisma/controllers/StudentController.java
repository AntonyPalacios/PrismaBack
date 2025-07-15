package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    ResponseEntity<StudentDTO> createStudent(@RequestBody StudentDTO studentDTO) {
        StudentDTO c = studentService.save(studentDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id,
                                             @RequestBody StudentDTO studentDTO) {
        StudentDTO c = studentService.update(id, studentDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }
    @DeleteMapping("/{id}")
    ResponseEntity<Long> deleteStudent(@PathVariable Long id) {
        Long deletedId = studentService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(deletedId);
    }

    @GetMapping
    ResponseEntity<List<StudentDTO>> getAllStudentsByStage(@RequestParam(name = "stageId") Long stageId,
                                                           @RequestParam(name = "userId", required = false) Optional<Long> userId) {
        List<StudentDTO> students = studentService.findAll(stageId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(students);
    }
}
