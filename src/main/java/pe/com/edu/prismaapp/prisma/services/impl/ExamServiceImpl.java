package pe.com.edu.prismaapp.prisma.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.ExamDTO;
import pe.com.edu.prismaapp.prisma.entities.*;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.CourseRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamCourseResultRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamResultRepository;
import pe.com.edu.prismaapp.prisma.services.ExamService;
import pe.com.edu.prismaapp.prisma.services.StageService;
import pe.com.edu.prismaapp.prisma.services.StudentService;
import pe.com.edu.prismaapp.prisma.services.StudentStageService;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final StageService stageService;
    private final StudentService studentService;
    private final StudentStageService studentStageService;
    private final ExamResultRepository examResultRepository;
    private final CourseRepository courseRepository;
    private final ExamCourseResultRepository examCourseResultRepository;

    public ExamServiceImpl(ExamRepository examRepository, StageService stageService, StudentService studentService, StudentStageService studentStageService,
                           ExamResultRepository examResultRepository, CourseRepository courseRepository,
                           ExamCourseResultRepository examCourseResultRepository) {
        this.examRepository = examRepository;
        this.stageService = stageService;
        this.studentService = studentService;
        this.studentStageService = studentStageService;
        this.examResultRepository = examResultRepository;
        this.courseRepository = courseRepository;
        this.examCourseResultRepository = examCourseResultRepository;
    }

    @Override
    public List<ExamDTO> getExams(Long cycleId, Long stageId) {
        List<Exam> exams = new ArrayList<>();
        List<ExamDTO> examsDto = new ArrayList<>();
        if (cycleId == null && stageId == null) {
            return examsDto;
        }

        //se busca por etapa
        if (cycleId == null) {
            exams = examRepository.findAllByStage_IdOrderByDateAsc(stageId);
        }

        //se listan todos los exams del ciclo
        if (stageId == null) {
            exams = examRepository.findAllByStage_Cycle_IdOrderByDateAsc(cycleId);
        }

        for (Exam exam : exams) {
            ExamDTO examDto = new ExamDTO();
            examDto.setId(exam.getId());
            examDto.setDate(exam.getDate());
            examDto.setName(exam.getName());
            examDto.setStage(exam.getStage().getName());
            examDto.setStageId(exam.getStage().getId());
            examDto.setCycle(exam.getStage().getCycle().getName());
            examsDto.add(examDto);
        }

        return examsDto;
    }

    @Override
    @Transactional
    public ExamDTO save(ExamDTO examDTO) {
        Exam exam = new Exam();
        exam.setName(examDTO.getName());
        exam.setDate(examDTO.getDate());
        if (examDTO.getStageId() != null) {
            Stage stage = stageService.getStageById(examDTO.getStageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Etapa no encontrada con ID: " + examDTO.getStageId()));
            exam.setStage(stage);
        } else {
            throw new RuntimeException("Seleccione etapa");
        }
        examRepository.save(exam);
        examDTO.setId(exam.getId());
        return examDTO;
    }

    @Override
    @Transactional
    public ExamDTO update(Long id, ExamDTO examDTO) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa no encontrada con ID: " + examDTO.getStageId()));
        exam.setName(examDTO.getName());
        exam.setDate(examDTO.getDate());
        examRepository.save(exam);
        return examDTO;
    }

    @Override
    @Transactional
    public void importResults(Long examId, AreaEnum area, MultipartFile file) throws IOException {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado con ID: " + examId));
        Long stageId = exam.getStage().getId();
        ExamResult examResult;

        //abrir excel y buscar página
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheet("EVA");
            //leer registros según area
            for (Row row : sheet) {
                if (row.getRowNum() < 11) {
                    continue;
                }

                String dni = dataFormatter.formatCellValue(row.getCell(2),evaluator).trim();
                String name = dataFormatter.formatCellValue(row.getCell(3),evaluator).trim();
                if(dni.length() < 8 || dni.equalsIgnoreCase("PROMEDIO") || name.isEmpty()) {
                    break;
                }
                Student student = studentService.findByDniOrName(dni, name);
                if (student == null) {
                    continue;
                }
                //por cada registro, buscar al alumno de la etapa y asignarlo a texamresult
                StudentStage studentStage = studentStageService.getStudentStage(stageId, student.getId());

                //aqui es donde se diferencia por cada area
                switch (area) {
                    case ARQUITECTURA: {
                        examResult = saveExamResult(28, 29, 30, exam, studentStage, row, dataFormatter,evaluator);
                        procesarExamArqui(examResult, row, dataFormatter,evaluator);
                        break;
                    }
                    case CIENCIAS: {
                        examResult = saveExamResult(31, 32, 33, exam, studentStage, row, dataFormatter,evaluator);
                        procesarExamCiencias(examResult, row, dataFormatter,evaluator);
                        break;
                    }
                    case LETRAS: {
                        examResult = saveExamResult(34, 35, 36, exam, studentStage, row, dataFormatter,evaluator);
                        procesarExamLetras(examResult, row, dataFormatter,evaluator);
                        break;
                    }
                }
            }
        }

    }


    private ExamResult saveExamResult(int i, int j, int k,
                                      Exam exam, StudentStage studentStage, Row row,
                                      DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        int merit = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(1),evaluator).trim());
        int totalCorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(i),evaluator).trim());
        int totalIncorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(j),evaluator).trim());
        double totalScore = Double.parseDouble(dataFormatter.formatCellValue(row.getCell(k),evaluator).trim());

        ExamResult examResult = new ExamResult(exam, studentStage, totalCorrect, totalIncorrect, totalScore, merit);
        examResultRepository.save(examResult);
        return examResult;
    }

    private void procesarExamArqui(ExamResult examResult, Row row, DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        //leer lectura
        saveExamCourseResult("LECT", 7, 8, 9, examResult, row, dataFormatter,evaluator);

        //leer mate
        saveExamCourseResult("NYO", 14, 15, 16, examResult, row, dataFormatter,evaluator);
        saveExamCourseResult("X", 17, 18, 19, examResult, row, dataFormatter,evaluator);
        saveExamCourseResult("GEO", 20, 21, 22, examResult, row, dataFormatter,evaluator);

    }

    private void procesarExamCiencias(ExamResult examResult, Row row, DataFormatter dataFormatter, FormulaEvaluator evaluator) {

        procesarExamArqui(examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("TRIGO", 23, 24, 25, examResult, row, dataFormatter,evaluator);
    }

    private void procesarExamLetras(ExamResult examResult, Row row, DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        procesarExamCiencias(examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("EST", 26, 27, 28, examResult, row, dataFormatter,evaluator);
    }

    private void saveExamCourseResult(String courseAbbreviation, int i, int j, int k, ExamResult examResult, Row row,
                                      DataFormatter dataFormatter, FormulaEvaluator evaluator) {

        Course course = courseRepository.findByAbbreviationAndParentCourseIsNotNull(courseAbbreviation);
        int courseCorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(i),evaluator).trim());
        int courseIncorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(j),evaluator).trim());
        int courseBlank = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(k),evaluator).trim());
        ExamCourseResult examCourseResult = new ExamCourseResult(examResult, course, courseCorrect, courseIncorrect, courseBlank);
        examCourseResultRepository.save(examCourseResult);
    }
}
