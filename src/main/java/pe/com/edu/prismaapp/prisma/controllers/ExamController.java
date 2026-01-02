package pe.com.edu.prismaapp.prisma.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.exam.*;
import pe.com.edu.prismaapp.prisma.services.ExamService;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/exams")
@Tag(name = "Gestión de Exámenes", description = "Operaciones relacionadas a los exámenes. " +
        "Contiene también los endpoints para obtener estadísticas de los exámenes")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @Operation(
            summary = "Listar exámenes por ciclo o etapa",
            description = "Lista todos los exámenes de un ciclo o etapa ordenadas por más reciente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exámenes listados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping
    ResponseEntity<List<ExamApi.ExamList>> getAllExams(@RequestParam(name="cycleId", required = false) Long cycleId,
                                                       @RequestParam(name = "stageId", required = false) Long stageId) {
        return ResponseEntity.ok(examService.getExams(cycleId, stageId));
    }

    @Operation(
            summary = "Crear un examen",
            description = "Crea un examen para la etapa dada"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Examen creado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe la etapa a la que se quiere crear examen", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @PostMapping
    ResponseEntity<ExamApi.Response> createExam(@RequestBody @Valid ExamApi.Create exam) {
        var e = examService.save(exam);
        return ResponseEntity.status(HttpStatus.OK).body(e);
    }

    @Operation(
            summary = "Actualizar un examen",
            description = "Actualiza un examen"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Examen actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe el examen que se quiere actualizar", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @PutMapping("/{id}")
    ResponseEntity<ExamApi.Response> update(@PathVariable Long id, @RequestBody @Valid ExamApi.Update exam) {
        var e = examService.update(id, exam);
        return ResponseEntity.status(HttpStatus.OK).body(e);
    }

    @Operation(
            summary = "Importar resultado de un examen"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos importados correctamete"),
            @ApiResponse(responseCode = "404", description = "No existe el examen a importar", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @PostMapping(path="/import/{id}/{area}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    ResponseEntity<Void> importExamResults(@PathVariable Long id, @PathVariable AreaEnum area, @RequestParam("file") MultipartFile file) throws IOException {
        examService.importResults(id, area, file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(
            summary = "Listar resultados de los exámenenes de un alumno"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos listados correctamete"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping("/results/{idStudent}/{idCycle}")
    ResponseEntity<List<ExamScore>> getExamResult(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamResultsByStudent(idStudent, idCycle));
    }

    @Operation(
            summary = "Listar efectivas por sección de los exámenenes de un alumno"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos listados correctamete"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping("/effective/{idStudent}/{idCycle}")
    ResponseEntity<List<ExamEffectiveSection>> getExamEffective(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamEffectiveByStudent(idStudent, idCycle));
    }

    @Operation(
            summary = "Listar efectivas por curso de los exámenenes de un alumno"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos listados correctamete"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping("/course/{idStudent}/{idCycle}")
    ResponseEntity<List<ExamEffectiveCourse>> getExamEffectiveCourse(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamEffectiveByCourseByStudent(idStudent, idCycle));
    }

    @Operation(
            summary = "Listar agregados para la charla de notas de un tutor"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos listados correctamete"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping("/summary/{areaId}/{userId}/{cycleId}")
    ResponseEntity<ExamDataSummary> getExamSummary(@PathVariable Long areaId, @PathVariable Long userId, @PathVariable Long cycleId) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamSummaryByTutor(areaId, userId, cycleId));
    }

    @Operation(
            summary = "Listar metas de los exámenes de un alumno"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos listados correctamete"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping("/goals/{studentId}/{cycleId}")
    ResponseEntity<List<ExamGoal>> getExamGoals(@PathVariable Long studentId, @PathVariable Long cycleId) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamGoalsByStudent(studentId, cycleId));
    }
}
