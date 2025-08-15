package pe.com.edu.prismaapp.prisma.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.ExamDTO;
import pe.com.edu.prismaapp.prisma.services.ExamService;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;

import java.io.IOException;

@RestController
@RequestMapping("/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    ResponseEntity<Object> getAllExams(@RequestParam(name="cycleId", required = false) Long cycleId,
                                       @RequestParam(name = "stageId", required = false) Long stageId) {
        return ResponseEntity.ok(examService.getExams(cycleId, stageId));
    }

    @PostMapping
    ResponseEntity<Object> createExam(@RequestBody @Valid ExamDTO exam) {
        ExamDTO e = examService.save(exam);
        return ResponseEntity.status(HttpStatus.OK).body(e);
    }

    @PutMapping("/{id}")
    ResponseEntity<Object> update(@PathVariable Long id, @RequestBody @Valid ExamDTO exam) {
        ExamDTO e = examService.update(id, exam);
        return ResponseEntity.status(HttpStatus.OK).body(e);
    }

    @PostMapping(path="/import/{id}/{area}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    ResponseEntity<Object> importExamResults(@PathVariable Long id, @PathVariable AreaEnum area, @RequestParam("file") MultipartFile file) throws IOException {
        examService.importResults(id, area, file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/results/{idStudent}/{idCycle}")
    ResponseEntity<Object> getExamResult(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamResultsByStudent(idStudent, idCycle));
    }

    @GetMapping("/effective/{idStudent}/{idCycle}")
    ResponseEntity<Object> getExamEffective(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamEffectiveByStudent(idStudent, idCycle));
    }

    @GetMapping("/course/{idStudent}/{idCycle}")
    ResponseEntity<Object> getExamEffectiveCourse(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamEffectiveByCourseByStudent(idStudent, idCycle));
    }

    @GetMapping("/summary/{areaId}/{userId}/{cycleId}")
    ResponseEntity<Object> getExamSummary(@PathVariable Long areaId, @PathVariable Long userId, @PathVariable Long cycleId) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamSummaryByTutor(areaId, userId, cycleId));
    }
}
