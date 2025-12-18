package pe.com.edu.prismaapp.prisma.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.exam.ExamApi;
import pe.com.edu.prismaapp.prisma.dto.exam.ExamEffectiveCourse;
import pe.com.edu.prismaapp.prisma.services.ExamService;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    ResponseEntity<List<ExamApi.ExamList>> getAllExams(@RequestParam(name="cycleId", required = false) Long cycleId,
                                                       @RequestParam(name = "stageId", required = false) Long stageId) {
        return ResponseEntity.ok(examService.getExams(cycleId, stageId));
    }

    @PostMapping
    ResponseEntity<ExamApi.Response> createExam(@RequestBody @Valid ExamApi.Create exam) {
        var e = examService.save(exam);
        return ResponseEntity.status(HttpStatus.OK).body(e);
    }

    @PutMapping("/{id}")
    ResponseEntity<ExamApi.Response> update(@PathVariable Long id, @RequestBody @Valid ExamApi.Update exam) {
        var e = examService.update(id, exam);
        return ResponseEntity.status(HttpStatus.OK).body(e);
    }

    @PostMapping(path="/import/{id}/{area}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    ResponseEntity<Void> importExamResults(@PathVariable Long id, @PathVariable AreaEnum area, @RequestParam("file") MultipartFile file) throws IOException {
        examService.importResults(id, area, file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/results/{idStudent}/{idCycle}")
    ResponseEntity<List<ExamApi.ExamScore>> getExamResult(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamResultsByStudent(idStudent, idCycle));
    }

    @GetMapping("/effective/{idStudent}/{idCycle}")
    ResponseEntity<List<ExamApi.ExamEffectiveSectionResponse>> getExamEffective(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamEffectiveByStudent(idStudent, idCycle));
    }

    @GetMapping("/course/{idStudent}/{idCycle}")
    ResponseEntity<List<ExamEffectiveCourse>> getExamEffectiveCourse(@PathVariable Long idStudent, @PathVariable Long idCycle) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamEffectiveByCourseByStudent(idStudent, idCycle));
    }

    @GetMapping("/summary/{areaId}/{userId}/{cycleId}")
    ResponseEntity<Object> getExamSummary(@PathVariable Long areaId, @PathVariable Long userId, @PathVariable Long cycleId) {
        return ResponseEntity.status(HttpStatus.OK).body(examService.getExamSummaryByTutor(areaId, userId, cycleId));
    }
}
