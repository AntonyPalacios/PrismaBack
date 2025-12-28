package pe.com.edu.prismaapp.prisma.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.StudentApi;
import pe.com.edu.prismaapp.prisma.services.StudentService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/students")
@Tag(name = "Gestión de Alumnos", description = "Operaciones relacionadas a los alumnos")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(
            summary = "Crear un alumno",
            description = "Crea un alumno asignandole una área, etapa (actual por defecto) y tutor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alumno creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TUTOR')")
    ResponseEntity<StudentApi.Response> createStudent(@Valid @RequestBody StudentApi.Create studentDTO) {
        var c = studentService.save(studentDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Actualizar un alumno",
            description = "Actualiza los datos de un alumno, cambiandole el área o tutor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alumno creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Alumno no existe o datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or " +
                  "(hasRole('TUTOR') and @studentServiceImpl.isStudentAssignedToTutor(#id,authentication.principal.id))")
    ResponseEntity<StudentApi.Response> updateStudent(@PathVariable Long id,
                                             @Valid @RequestBody StudentApi.Update studentDTO) {
        var c = studentService.update(id, studentDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Eliminar un alumno",
            description = "Elimina todos los datos relacionados a un alumno."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alumno eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "Alumno no existe", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Listar los alumnos de una etapa",
            description = "Lista los alumnos de una etapa para el usuario que hizo la solicitud. " +
                    "Si el parámetro stageId no esta presente, se devuleven todos los alumnos. " +
                    "Si el usuario es ADMIN, se devuelven todos los alumnos de todos los tutores."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alumnos listados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @GetMapping
    ResponseEntity<List<StudentApi.Response>> getAllStudentsByStage(@RequestParam(name = "stageId", required = false) Optional<Long> stageId) {
        var students = studentService.findAll(stageId);
        return ResponseEntity.status(HttpStatus.OK).body(students);
    }

    @Operation(
            summary = "Importar una lista de alumnos",
            description = "Importa una lista de alumnos a la etapa actual siguiendo un archivo excel definido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alumnos importados correctamente"),
            @ApiResponse(responseCode = "400", description = "No existe una etapa actual o el archivo es incorrecto", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path="/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    ResponseEntity<Object> uploadStudents(@RequestParam("file") MultipartFile file) {
        studentService.uploadStudents(file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(
            summary = "Descargar plantilla para importar alumnos"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plantilla descargada correctamente"),
            @ApiResponse(responseCode = "404", description = "Plantilla no existe", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
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
