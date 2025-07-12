package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.dto.UserDTO;
import pe.com.edu.prismaapp.prisma.services.StudentService;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    ResponseEntity<StudentDTO> createUser(@RequestBody StudentDTO studentDTO) {
        StudentDTO c = studentService.save(studentDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @GetMapping
    ResponseEntity<List<StudentDTO>> getAllStudentsByStage(@RequestParam(name = "idStage",required = false) Long idStage) {
        List<StudentDTO> students = studentService.findAll(idStage);
        return ResponseEntity.status(HttpStatus.OK).body(students);
    }
}
