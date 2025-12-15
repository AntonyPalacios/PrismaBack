package pe.com.edu.prismaapp.prisma.services.impl;

import org.apache.commons.lang3.function.TriConsumer;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.*;
import pe.com.edu.prismaapp.prisma.dto.exam.*;
import pe.com.edu.prismaapp.prisma.entities.*;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.CourseRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamCourseResultRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamResultRepository;
import pe.com.edu.prismaapp.prisma.services.*;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;
import pe.com.edu.prismaapp.prisma.util.ExamColumnsUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final StageService stageService;
    private final StudentService studentService;
    private final StudentStageService studentStageService;
    private final ExamResultRepository examResultRepository;
    private final CourseRepository courseRepository;
    private final ExamCourseResultRepository examCourseResultRepository;
    private final AreaService areaService;
    private final UserService userService;
    private FormulaEvaluator evaluator = null;
    private final ExamColumnsUtil.CourseColumns courseColumns;
    private final ExamColumnsUtil.AreaConfig arquiConfig;
    private final ExamColumnsUtil.AreaConfig cienciasConfig;
    private final ExamColumnsUtil.AreaConfig letrasConfig;

    public ExamServiceImpl(ExamRepository examRepository, StageService stageService, StudentService studentService,
                           StudentStageService studentStageService, ExamResultRepository examResultRepository,
                           CourseRepository courseRepository, ExamCourseResultRepository examCourseResultRepository,
                           AreaService areaService, UserService userService, ExamColumnsUtil.CourseColumns courseColumns,
                           @Qualifier("arqui") ExamColumnsUtil.AreaConfig arquiConfig,
                           @Qualifier("ciencias") ExamColumnsUtil.AreaConfig cienciasConfig,
                           @Qualifier("letras") ExamColumnsUtil.AreaConfig letrasConfig) {
        this.examRepository = examRepository;
        this.stageService = stageService;
        this.studentService = studentService;
        this.studentStageService = studentStageService;
        this.examResultRepository = examResultRepository;
        this.courseRepository = courseRepository;
        this.examCourseResultRepository = examCourseResultRepository;
        this.areaService = areaService;
        this.userService = userService;
        this.courseColumns = courseColumns;
        this.arquiConfig = arquiConfig;
        this.cienciasConfig = cienciasConfig;
        this.letrasConfig = letrasConfig;
    }

    @Override
    public List<ExamApi.ExamList> getExams(Long cycleId, Long stageId) {
        List<Exam> exams = new ArrayList<>();
        var examsDto = new ArrayList<ExamApi.ExamList>();
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

        return exams.stream().map(exam -> new ExamApi.ExamList(
                exam.getId(),
                exam.getName(),
                exam.getStage().getId(), exam.getDate(),
                exam.getStage().getCycle().getName(),
                exam.getStage().getName())
        ).toList();

    }

    @Override
    @Transactional
    public ExamApi.Response save(ExamApi.Create examDTO) {
        Exam exam = new Exam();
        exam.setName(examDTO.name());
        exam.setDate(examDTO.date());
        if (examDTO.stageId() != null) {
            Stage stage = stageService.getStageById(examDTO.stageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado con ID: " + examDTO.stageId()));
            exam.setStage(stage);
        } else {
            throw new RuntimeException("Seleccione etapa");
        }
        examRepository.save(exam);
        return ExamApi.Response.from(exam);
    }

    @Override
    @Transactional
    public ExamApi.Response update(Long id, ExamApi.Update examDTO) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado con ID: " + examDTO.id()));
        exam.setName(examDTO.name());
        exam.setDate(examDTO.date());
        examRepository.save(exam);
        return ExamApi.Response.from(exam);
    }

    @Override
    @Transactional
    public void importResults(Long examId, AreaEnum area, MultipartFile file) throws IOException {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado con ID: " + examId));
        Long stageId = exam.getStage().getId();
        ExamResult examResult;

        TriConsumer<ExamResult, Row, DataFormatter> processor;
        ExamColumnsUtil.AreaConfig config;

        //abrir excel y buscar página
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            DataFormatter dataFormatter = new DataFormatter();
            this.evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheet("EVA");
            //leer registros según area
            for (Row row : sheet) {
                if (row.getRowNum() < 11) {
                    continue;
                }

                String dni = dataFormatter.formatCellValue(row.getCell(2), evaluator).trim();
                String name = dataFormatter.formatCellValue(row.getCell(3), evaluator).trim();
                if (dni.equalsIgnoreCase("PROMEDIO") || name.length() < 3) {
                    break;
                }

                //aqui es donde se diferencia por cada area
                switch (area) {
                    case ARQUITECTURA -> {
                        config = arquiConfig;
                        processor = this::procesarExamArqui;
                    }
                    case CIENCIAS -> {
                        config = cienciasConfig;
                        processor = this::procesarExamCiencias;
                    }
                    case LETRAS -> {
                        config = letrasConfig;
                        processor = this::procesarExamLetras;
                    }
                    default -> throw new IllegalArgumentException("Area no soportada: " + area);
                }
                var studentStage = getStudentStage(area, config.tutorCol(), name, stageId, row, dataFormatter, evaluator);
                examResult = saveExamResult(config.goodCol(), config.badCol(), config.scoreCol(), exam, studentStage, row, area, dataFormatter, evaluator);
                processor.accept(examResult, row, dataFormatter);
            }
        }

    }


    private StudentStage getStudentStage(AreaEnum area, int tutorColumnIndex, String name, Long stageId, Row row, DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        Student student = studentService.findByDniOrName(null, name);
        String tutor = dataFormatter.formatCellValue(row.getCell(tutorColumnIndex), evaluator).trim();

        Optional<User> optionalTutor = userService.findTutorByName(tutor);
        Long tutorId = optionalTutor.isPresent() ? optionalTutor.get().getId() : 0L;

        StudentApi.Response studentDTO = null;
        if (student == null) {
            //crear alumno
            Optional<Area> optionalArea = areaService.findAreaByName(area.name());
            Long areaId = optionalArea.isPresent() ? optionalArea.get().getId() : 0L;
            StudentApi.Create studentCreate = new StudentApi.Create(name, "", "", "", tutorId, areaId, stageId, true);

            studentDTO = studentService.save(studentCreate);
        }
        //por cada registro, buscar al alumno de la etapa y asignarlo a texamresult
        Long studentId = student == null ? studentDTO.id() : student.getId();
        StudentStage studentStage = studentStageService.getStudentStage(stageId, studentId);

        if (student != null) {
            //si estamos en una nueva etapa, se crean los registros
            if (studentStage == null) {
                studentStage = saveStudentStage(studentId, stageId);
                //studentStage = studentStageService.getStudentStage(stageId,studentId);
            }
            studentStageService.validateStudentTutor(studentStage.getId(), tutorId);
        }
        return studentStage;
    }

    private StudentStage saveStudentStage(Long studentId, Long stageId) {
        Student student = studentService.findById(studentId);
        Optional<Stage> optStage = stageService.getStageById(stageId);
        return studentStageService.saveStudent(student, optStage.get(), true);
    }


    private ExamResult saveExamResult(int i, int j, int k,
                                      Exam exam, StudentStage studentStage, Row row, AreaEnum areaEnum,
                                      DataFormatter dataFormatter, FormulaEvaluator evaluator) {

        int merit = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(1), evaluator).trim());
        int totalCorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(i), evaluator).trim());
        int totalIncorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(j), evaluator).trim());
        double totalScore = Double.parseDouble(dataFormatter.formatCellValue(row.getCell(k), evaluator).trim());
        if (totalScore == Double.parseDouble("80.00") || totalCorrect <= 1) { //si no dio examen, no se ingresa
            return null;
        }
        Area area = areaService.findAreaByName(areaEnum.name()).orElseThrow(() -> new ResourceNotFoundException("Area: " + areaEnum.name()));
        //verifica si ya existe para actualizarlo
        System.out.println("Exam: " + exam.getId());
        System.out.println("StudentStage: " + studentStage.getId());
        ExamResult examResult = examResultRepository.findExamResultByExam_IdAndStudentStage_Id(exam.getId(), studentStage.getId())
                .orElse(null);
        if (examResult == null) {
            examResult = new ExamResult(exam, studentStage, area, totalCorrect, totalIncorrect, totalScore, merit);
        } else {
            examResult.setMerit(merit);
            examResult.setArea(area);
            examResult.setTotalCorrect(totalCorrect);
            examResult.setTotalIncorrect(totalIncorrect);
            examResult.setTotalScore(totalScore);
        }
        examResultRepository.save(examResult);
        return examResult;
    }

    private void procesarExamArqui(ExamResult examResult, Row row, DataFormatter dataFormatter) {
        if (examResult == null) {
            return;
        }

        //leer lectura
        saveExamCourseResult("LECT",
                courseColumns.lectCorrect(),
                courseColumns.lectIncorrect(),
                courseColumns.lectBlank(),
                examResult, row, dataFormatter, evaluator);

        //leer mate
        saveExamCourseResult("NYO",
                courseColumns.nyoCorrect(),
                courseColumns.nyoIncorrect(),
                courseColumns.nyoBlank(),
                examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("X",
                courseColumns.xCorrect(),
                courseColumns.xIncorrect(),
                courseColumns.xBlank(),
                examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("GEO",
                courseColumns.geoCorrect(),
                courseColumns.geoIncorrect(),
                courseColumns.geoBlank(),
                examResult, row, dataFormatter, evaluator);

    }

    private void procesarExamCiencias(ExamResult examResult, Row row,
                                      DataFormatter dataFormatter) {

        procesarExamArqui(examResult, row, dataFormatter);
        saveExamCourseResult("TRIGO",
                courseColumns.trigoCorrect(),
                courseColumns.trigoIncorrect(),
                courseColumns.trigoBlank(),
                examResult, row, dataFormatter, evaluator);
    }

    private void procesarExamLetras(ExamResult examResult, Row row,
                                    DataFormatter dataFormatter) {
        procesarExamCiencias(examResult, row, dataFormatter);
        saveExamCourseResult("EST",
                courseColumns.estCorrect(),
                courseColumns.estIncorrect(),
                courseColumns.estBlank(),
                examResult, row, dataFormatter, evaluator);
    }

    private void saveExamCourseResult(String courseAbbreviation, int i, int j, int k,
                                      ExamResult examResult, Row row,
                                      DataFormatter dataFormatter, FormulaEvaluator evaluator) {

        if (examResult == null) {
            return;
        }
        Course course = courseRepository.findByAbbreviationAndParentCourseIsNotNull(courseAbbreviation);
        int courseCorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(i), evaluator).trim());
        int courseIncorrect = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(j), evaluator).trim());
        int courseBlank = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(k), evaluator).trim());

        ExamCourseResult examCourseResult = examCourseResultRepository
                .findExamCourseResultByCourse_IdAndExamResult_Id(course.getId(), examResult.getId())
                .orElse(null);

        if (examCourseResult == null) {
            examCourseResult = new ExamCourseResult(examResult, course, courseCorrect, courseIncorrect, courseBlank);
        } else {
            examCourseResult.setCourseCorrect(courseCorrect);
            examCourseResult.setCourseIncorrect(courseIncorrect);
            examCourseResult.setCourseBlank(courseBlank);
        }
        examCourseResultRepository.save(examCourseResult);
    }

    @Override
    public List<ExamApi.ExamScore> getExamResultsByStudent(Long studentId, Long cycleId) {
        var examResults = examResultRepository.listExamResultsByStudent(studentId, cycleId);
        return examResults.stream().map(e -> {
            var examId = e.examId();
            var areaId = e.areaId();
            List<Object[]> data = examResultRepository.getMinMaxAndAvgByExamByArea(examId, areaId);
            return new ExamApi.ExamScore(
                    e.name(),
                    e.totalScore(),
                    (Double) data.get(0)[0],
                    (Double) data.get(0)[1],
                    (Double) data.get(0)[2],
                    e.merit()
            );
        }).toList();
    }

    @Override
    public List<ExamApi.ExamEffectiveSectionResponse> getExamEffectiveByStudent(Long idStudent, Long idCycle) {
        List<ExamApi.ExamEffectiveSectionResponse> examScoreDTOs = new ArrayList<>();
        //sacar buenas y malas de lectura y matemática
        List<ExamEffectiveSection> lectura =
                examResultRepository.listExamEffectiveByStudent(idStudent, idCycle, 1L);

        List<ExamEffectiveSection> mate =
                examResultRepository.listExamEffectiveByStudent(idStudent, idCycle, 2L);
        // TODO: verificar forma de unir ambos streams en tuplas
        if (lectura.size() == mate.size()) {
            for (int i = 0; i < lectura.size(); i++) {
                ExamApi.ExamEffectiveSectionResponse examScoreDTO
                        = new ExamApi.ExamEffectiveSectionResponse(
                        lectura.get(i).name(),
                        lectura.get(i).totalCorrect().intValue(),
                        lectura.get(i).totalIncorrect().intValue(),
                        mate.get(i).totalCorrect().intValue(),
                        mate.get(i).totalIncorrect().intValue()
                );
                examScoreDTOs.add(examScoreDTO);
            }
            return examScoreDTOs;
        }
        return examScoreDTOs;

    }

    @Override
    public List<ExamCourseResultDTO> getExamEffectiveByCourseByStudent(Long studentId, Long cycleId) {
        List<ExamCourseResultDTO> examCourseResultDTOS = new ArrayList<>();

        examRepository.getExamsWithResults(cycleId).forEach(exam -> {
            ExamCourseResultDTO examCourseResultDTO = new ExamCourseResultDTO();
            courseRepository.findByParentCourseIsNotNull().forEach(course -> {
                examResultRepository.listExamEffectiveByCourseByStudent(studentId, course.getId(), exam.getId()).forEach(examResult -> {
                    examCourseResultDTO.setName(examResult.name());
                    validateCourse(examResult, examCourseResultDTO, course.getAbbreviation());
                });

            });
            examCourseResultDTOS.add(examCourseResultDTO);
        });

        return examCourseResultDTOS;
    }

    private void validateCourse(ExamEffectiveCourse examResult, ExamCourseResultDTO examCourseResultDTO, String abbreviation) {
        switch (abbreviation) {
            case "LECT" -> {
                examCourseResultDTO.setLectCorrect(examResult.totalCorrect());
                examCourseResultDTO.setLectIncorrect(examResult.totalIncorrect());
            }
            case "NYO" -> {
                examCourseResultDTO.setNyoCorrect(examResult.totalCorrect());
                examCourseResultDTO.setNyoIncorrect(examResult.totalIncorrect());
            }
            case "X" -> {
                examCourseResultDTO.setXCorrect(examResult.totalCorrect());
                examCourseResultDTO.setXIncorrect(examResult.totalIncorrect());
            }

            case "GEO" -> {
                examCourseResultDTO.setGeoCorrect(examResult.totalCorrect());
                examCourseResultDTO.setGeoIncorrect(examResult.totalIncorrect());
            }

            case "TRIGO" -> {
                examCourseResultDTO.setTrigoCorrect(examResult.totalCorrect());
                examCourseResultDTO.setTrigoIncorrect(examResult.totalIncorrect());
            }

            case "EST" -> {
                examCourseResultDTO.setEstCorrect(examResult.totalCorrect());
                examCourseResultDTO.setEstIncorrect(examResult.totalIncorrect());
            }

        }
    }

    @Override
    public ExamDataSummary getExamSummaryByTutor(Long areaId, Long userId, Long cycleId) {
        ExamDataSummary examDataSummary = new ExamDataSummary();

        List<ExamData> examDatas = new ArrayList<>();
        List<ExamIndicator> lectData = new ArrayList<>();
        List<ExamIndicator> mateData = new ArrayList<>();

        if (areaId == -1) {
            areaId = null;
        }


        List<List<Object[]>> allMinLect = new ArrayList<>();
        List<List<Object[]>> allMaxLect = new ArrayList<>();
        List<List<Object[]>> allAvgLect = new ArrayList<>();

        List<List<Object[]>> allMinMate = new ArrayList<>();
        List<List<Object[]>> allMaxMate = new ArrayList<>();
        List<List<Object[]>> allAvgMate = new ArrayList<>();

        //listar todos los examenes con resultados del ciclo
        Long finalAreaId = areaId;
        Pageable topFour = PageRequest.of(0, 4);
        List<Exam> lastFourExams = examRepository.getExamsWithResultsLastFour(cycleId, topFour);
        Collections.reverse(lastFourExams);
        lastFourExams.forEach(exam -> {
            ExamData examData = new ExamData();
            examData.setId(exam.getId());
            examData.setName(exam.getName());
            examData.setDate(exam.getDate());
            examDatas.add(examData);

            //sacar agregados de lectura para el area por usuario
            List<Object[]> minLect = examResultRepository.getMinExamResult(exam.getId(), finalAreaId, userId, 1L);
            List<Object[]> maxLect = examResultRepository.getMaxExamResult(exam.getId(), finalAreaId, userId, 1L);
            List<Object[]> avgLect = examResultRepository.getAvgExamResult(exam.getId(), finalAreaId, userId, 1L);

            //sacar agregados de mate para el area por usuario
            List<Object[]> minMate = examResultRepository.getMinExamResult(exam.getId(), finalAreaId, userId, 2L);
            List<Object[]> maxMate = examResultRepository.getMaxExamResult(exam.getId(), finalAreaId, userId, 2L);
            List<Object[]> avgMate = examResultRepository.getAvgExamResult(exam.getId(), finalAreaId, userId, 2L);

            allMinLect.add(minLect);
            allMaxLect.add(maxLect);
            allAvgLect.add(avgLect);

            allMinMate.add(minMate);
            allMaxMate.add(maxMate);
            allAvgMate.add(avgMate);

        });
        examDataSummary.setExamData(examDatas);

        setExamIndicator("Min", allMinLect, lectData);
        setExamIndicator("Max", allMaxLect, lectData);
        setExamIndicator("Avg", allAvgLect, lectData);

        setExamIndicator("Min", allMinMate, mateData);
        setExamIndicator("Max", allMaxMate, mateData);
        setExamIndicator("Avg", allAvgMate, mateData);

        examDataSummary.setLectData(lectData);
        examDataSummary.setMateData(mateData);

        return examDataSummary;
    }

    private void setExamIndicator(String indicator, List<List<Object[]>> allData, List<ExamIndicator> data) {
        ExamIndicator examIndicator = new ExamIndicator();
        List<ExamResultIndicator> examResultIndicators = new ArrayList<>();
        examIndicator.setIndicator(indicator);
        for (List<Object[]> listData : allData) {
            for (Object[] result : listData) {
                ExamResultIndicator examResultIndicator = new ExamResultIndicator();
                examResultIndicator.setExamId((Long) result[0]);
                examResultIndicator.setCorrect(Integer.parseInt(String.valueOf(result[1])));
                examResultIndicator.setIncorrect(Integer.parseInt(String.valueOf(result[2])));
                examResultIndicators.add(examResultIndicator);
            }
        }
        examIndicator.setResults(examResultIndicators);
        data.add(examIndicator);
    }
}
