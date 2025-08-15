package pe.com.edu.prismaapp.prisma.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.*;
import pe.com.edu.prismaapp.prisma.entities.*;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.CourseRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamCourseResultRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamRepository;
import pe.com.edu.prismaapp.prisma.repositories.ExamResultRepository;
import pe.com.edu.prismaapp.prisma.services.*;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

    public ExamServiceImpl(ExamRepository examRepository, StageService stageService, StudentService studentService, StudentStageService studentStageService,
                           ExamResultRepository examResultRepository, CourseRepository courseRepository,
                           ExamCourseResultRepository examCourseResultRepository, AreaService areaService, UserService userService) {
        this.examRepository = examRepository;
        this.stageService = stageService;
        this.studentService = studentService;
        this.studentStageService = studentStageService;
        this.examResultRepository = examResultRepository;
        this.courseRepository = courseRepository;
        this.examCourseResultRepository = examCourseResultRepository;
        this.areaService = areaService;
        this.userService = userService;
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

                String dni = dataFormatter.formatCellValue(row.getCell(2), evaluator).trim();
                String name = dataFormatter.formatCellValue(row.getCell(3), evaluator).trim();
                if (dni.equalsIgnoreCase("PROMEDIO") || name.length() < 3) {
                    break;
                }

                //aqui es donde se diferencia por cada area
                switch (area) {
                    case ARQUITECTURA: {
                        StudentStage studentStage = getStudentStage(area, 32, name, stageId, row, dataFormatter, evaluator);
                        examResult = saveExamResult(28, 29, 30, exam, studentStage, row, area, dataFormatter, evaluator);
                        procesarExamArqui(examResult, row, dataFormatter, evaluator);
                        break;
                    }
                    case CIENCIAS: {
                        StudentStage studentStage = getStudentStage(area, 35, name, stageId, row, dataFormatter, evaluator);
                        examResult = saveExamResult(31, 32, 33, exam, studentStage, row, area, dataFormatter, evaluator);
                        procesarExamCiencias(examResult, row, dataFormatter, evaluator);
                        break;
                    }
                    case LETRAS: {
                        StudentStage studentStage = getStudentStage(area, 38, name, stageId, row, dataFormatter, evaluator);
                        examResult = saveExamResult(34, 35, 36, exam, studentStage, row, area, dataFormatter, evaluator);
                        procesarExamLetras(examResult, row, dataFormatter, evaluator);
                        break;
                    }
                }
            }
        }

    }

    private StudentStage getStudentStage(AreaEnum area, int i, String name, Long stageId, Row row, DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        Student student = studentService.findByDniOrName(null, name);
        String tutor = dataFormatter.formatCellValue(row.getCell(i), evaluator).trim();
        StudentDTO studentDTO = new StudentDTO();
        if (student == null) {
            //crear alumno
            studentDTO.setDni("");
            studentDTO.setName(name);
            studentDTO.setStageId(stageId);
            Optional<Area> optionalArea = areaService.findAreaByName(area.name());
            Optional<User> optionalTutor = userService.findTutorByName(tutor);

            if (optionalTutor.isPresent()) {
                studentDTO.setTutorId(optionalTutor.get().getId());
            } else {
                studentDTO.setTutorId(0L);
            }

            if (optionalArea.isPresent()) {
                studentDTO.setAreaId(optionalArea.get().getId());
            } else {
                studentDTO.setAreaId(0L);
            }
            studentDTO = studentService.save(studentDTO);
        }
        //por cada registro, buscar al alumno de la etapa y asignarlo a texamresult
        return studentStageService.getStudentStage(stageId,
                student == null ? studentDTO.getId() : student.getId());
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

    private void procesarExamArqui(ExamResult examResult, Row row,
                                   DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        if (examResult == null) {
            return;
        }

        //leer lectura
        saveExamCourseResult("LECT", 7, 8, 9, examResult, row, dataFormatter, evaluator);

        //leer mate
        saveExamCourseResult("NYO", 14, 15, 16, examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("X", 17, 18, 19, examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("GEO", 20, 21, 22, examResult, row, dataFormatter, evaluator);

    }

    private void procesarExamCiencias(ExamResult examResult, Row row,
                                      DataFormatter dataFormatter, FormulaEvaluator evaluator) {

        procesarExamArqui(examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("TRIGO", 23, 24, 25, examResult, row, dataFormatter, evaluator);
    }

    private void procesarExamLetras(ExamResult examResult, Row row,
                                    DataFormatter dataFormatter, FormulaEvaluator evaluator) {
        procesarExamCiencias(examResult, row, dataFormatter, evaluator);
        saveExamCourseResult("EST", 26, 27, 28, examResult, row, dataFormatter, evaluator);
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
    public List<ExamScoreDTO> getExamResultsByStudent(Long studentId, Long cycleId) {
        List<ExamResultDTO> examResults = examResultRepository.listExamResultsByStudent(studentId, cycleId);
        List<ExamScoreDTO> examScoreDTOs = new ArrayList<>();
        for (ExamResultDTO examResultDTO : examResults) {
            ExamScoreDTO examScoreDTO = new ExamScoreDTO();
            Long examId = examResultDTO.getExamId();
            Long areaId = examResultDTO.getAreaId();
            List<Object[]> data = examResultRepository.getMinMaxAndAvgByExamByArea(examId, areaId);

            examScoreDTO.setName(examResultDTO.getName());
            examScoreDTO.setMerit(examResultDTO.getMerit());
            examScoreDTO.setScore(examResultDTO.getTotalScore());
            examScoreDTO.setMin((Double) data.get(0)[0]);
            examScoreDTO.setMax((Double) data.get(0)[1]);
            examScoreDTO.setAvg((Double) data.get(0)[2]);

            examScoreDTOs.add(examScoreDTO);
        }
        return examScoreDTOs;
    }

    @Override
    public List<ExamScoreDTO> getExamEffectiveByStudent(Long idStudent, Long idCycle) {
        List<ExamScoreDTO> examScoreDTOs = new ArrayList<>();
        //sacar buenas y malas de lectura y matemática
        List<ExamScoreDTO> lectura = examResultRepository.listExamEffectiveByStudent(idStudent, idCycle, 1L);
        List<ExamScoreDTO> mate = examResultRepository.listExamEffectiveByStudent(idStudent, idCycle, 2L);

        if (lectura.size() == mate.size()) {
            for (int i = 0; i < lectura.size(); i++) {
                ExamScoreDTO examScoreDTO = new ExamScoreDTO();
                examScoreDTO.setName(lectura.get(i).getName());
                examScoreDTO.setTotalLectCorrect(lectura.get(i).getTotalCorrect());
                examScoreDTO.setTotalLectIncorrect(lectura.get(i).getTotalIncorrect());
                examScoreDTO.setTotalMateCorrect(mate.get(i).getTotalCorrect());
                examScoreDTO.setTotalMateIncorrect(mate.get(i).getTotalIncorrect());
                examScoreDTOs.add(examScoreDTO);
            }
            return examScoreDTOs;
        }
        return examScoreDTOs;

    }

    /*@Override
    public List<ExamCourseResultDTO> getExamEffectiveByCourseByStudent(Long idStudent, Long idCycle) {
        List<ExamCourseResultDTO> examCourseResultDTOS = new ArrayList<>();
        Course lect = courseRepository.findByAbbreviationAndParentCourseIsNotNull("LECT");
        Course nyo = courseRepository.findByAbbreviationAndParentCourseIsNotNull("NYO");
        Course x = courseRepository.findByAbbreviationAndParentCourseIsNotNull("X");
        Course geo = courseRepository.findByAbbreviationAndParentCourseIsNotNull("GEO");
        Course trigo = courseRepository.findByAbbreviationAndParentCourseIsNotNull("TRIGO");
        Course est = courseRepository.findByAbbreviationAndParentCourseIsNotNull("EST");

        //sacar buenas y malas de lectura y matemática
        List<ExamScoreDTO> lectura = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, lect.getId());
        List<ExamScoreDTO> nyoExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, nyo.getId());
        List<ExamScoreDTO> xExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, x.getId());
        List<ExamScoreDTO> geoExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, geo.getId());
        List<ExamScoreDTO> trigoExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, trigo.getId());
        List<ExamScoreDTO> estExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, est.getId());


        for (int i = 0; i < lectura.size(); i++) {
            ExamCourseResultDTO examScoreDTO = new ExamCourseResultDTO();
            examScoreDTO.setName(lectura.get(i).getName());

            examScoreDTO.setLectCorrect(lectura.get(i).getTotalCorrect());
            examScoreDTO.setLectIncorrect(lectura.get(i).getTotalIncorrect());

            examScoreDTO.setNyoCorrect(nyoExam.get(i).getTotalCorrect());
            examScoreDTO.setNyoIncorrect(nyoExam.get(i).getTotalIncorrect());

            examScoreDTO.setXCorrect(xExam.get(i).getTotalCorrect());
            examScoreDTO.setXIncorrect(xExam.get(i).getTotalIncorrect());

            examScoreDTO.setGeoCorrect(geoExam.get(i).getTotalCorrect());
            examScoreDTO.setGeoIncorrect(geoExam.get(i).getTotalIncorrect());

            if(!trigoExam.isEmpty()){
                examScoreDTO.setTrigoCorrect(trigoExam.get(i).getTotalCorrect());
                examScoreDTO.setTrigoIncorrect(trigoExam.get(i).getTotalIncorrect());
            }

            if(!estExam.isEmpty()){
                examScoreDTO.setEstCorrect(estExam.get(i).getTotalCorrect());
                examScoreDTO.setEstIncorrect(estExam.get(i).getTotalIncorrect());
            }

            examCourseResultDTOS.add(examScoreDTO);
        }
        return examCourseResultDTOS;


    }*/

    @Override
    public List<ExamCourseResultDTO> getExamEffectiveByCourseByStudent(Long studentId, Long cycleId) {
        List<ExamCourseResultDTO> examCourseResultDTOS = new ArrayList<>();

        examRepository.getExamsWithResults(cycleId).forEach(exam -> {
            ExamCourseResultDTO examCourseResultDTO = new ExamCourseResultDTO();
            courseRepository.findByParentCourseIsNotNull().forEach(course -> {
                examResultRepository.listExamEffectiveByCourseByStudent(studentId, course.getId(),exam.getId()).forEach(examResult -> {
                    examCourseResultDTO.setName(examResult.getName());
                    validateCourse(examResult, examCourseResultDTO, course.getAbbreviation());
                });

            });
            examCourseResultDTOS.add(examCourseResultDTO);
        });

        return examCourseResultDTOS;

        /*Course lect = courseRepository.findByAbbreviationAndParentCourseIsNotNull("LECT");
        Course nyo = courseRepository.findByAbbreviationAndParentCourseIsNotNull("NYO");
        Course x = courseRepository.findByAbbreviationAndParentCourseIsNotNull("X");
        Course geo = courseRepository.findByAbbreviationAndParentCourseIsNotNull("GEO");
        Course trigo = courseRepository.findByAbbreviationAndParentCourseIsNotNull("TRIGO");
        Course est = courseRepository.findByAbbreviationAndParentCourseIsNotNull("EST");

        //sacar buenas y malas de lectura y matemática
        List<ExamScoreDTO> lectura = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, lect.getId());
        List<ExamScoreDTO> nyoExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, nyo.getId());
        List<ExamScoreDTO> xExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, x.getId());
        List<ExamScoreDTO> geoExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, geo.getId());
        List<ExamScoreDTO> trigoExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, trigo.getId());
        List<ExamScoreDTO> estExam = examResultRepository.listExamEffectiveByCourseByStudent(idStudent, idCycle, est.getId());


        for (int i = 0; i < lectura.size(); i++) {
            ExamCourseResultDTO examScoreDTO = new ExamCourseResultDTO();
            examScoreDTO.setName(lectura.get(i).getName());

            examScoreDTO.setLectCorrect(lectura.get(i).getTotalCorrect());
            examScoreDTO.setLectIncorrect(lectura.get(i).getTotalIncorrect());

            examScoreDTO.setNyoCorrect(nyoExam.get(i).getTotalCorrect());
            examScoreDTO.setNyoIncorrect(nyoExam.get(i).getTotalIncorrect());

            examScoreDTO.setXCorrect(xExam.get(i).getTotalCorrect());
            examScoreDTO.setXIncorrect(xExam.get(i).getTotalIncorrect());

            examScoreDTO.setGeoCorrect(geoExam.get(i).getTotalCorrect());
            examScoreDTO.setGeoIncorrect(geoExam.get(i).getTotalIncorrect());

            if (!trigoExam.isEmpty()) {
                examScoreDTO.setTrigoCorrect(trigoExam.get(i).getTotalCorrect());
                examScoreDTO.setTrigoIncorrect(trigoExam.get(i).getTotalIncorrect());
            }

            if (!estExam.isEmpty()) {
                examScoreDTO.setEstCorrect(estExam.get(i).getTotalCorrect());
                examScoreDTO.setEstIncorrect(estExam.get(i).getTotalIncorrect());
            }

            examCourseResultDTOS.add(examScoreDTO);
        }
        return examCourseResultDTOS;*/


    }

    private void validateCourse(ExamScoreDTO examResult, ExamCourseResultDTO examCourseResultDTO, String abbreviation) {
        switch (abbreviation) {
            case "LECT":
                examCourseResultDTO.setLectCorrect(examResult.getTotalCorrect());
                examCourseResultDTO.setLectIncorrect(examResult.getTotalIncorrect());
                break;
            case "NYO":
                examCourseResultDTO.setNyoCorrect(examResult.getTotalCorrect());
                examCourseResultDTO.setNyoIncorrect(examResult.getTotalIncorrect());
                break;
            case "X":
                examCourseResultDTO.setXCorrect(examResult.getTotalCorrect());
                examCourseResultDTO.setXIncorrect(examResult.getTotalIncorrect());
                break;
            case "GEO":
                examCourseResultDTO.setGeoCorrect(examResult.getTotalCorrect());
                examCourseResultDTO.setGeoIncorrect(examResult.getTotalIncorrect());
                break;
            case "TRIGO":
                examCourseResultDTO.setTrigoCorrect(examResult.getTotalCorrect());
                examCourseResultDTO.setTrigoIncorrect(examResult.getTotalIncorrect());
                break;
            case "EST":
                examCourseResultDTO.setEstCorrect(examResult.getTotalCorrect());
                examCourseResultDTO.setEstIncorrect(examResult.getTotalIncorrect());
                break;
            default:
                break;

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
        examRepository.getExamsWithResults(cycleId).forEach(exam -> {
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
