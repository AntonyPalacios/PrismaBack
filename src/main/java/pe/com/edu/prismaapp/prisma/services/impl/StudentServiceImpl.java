package pe.com.edu.prismaapp.prisma.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.auth.CustomUserDetails;
import pe.com.edu.prismaapp.prisma.dto.StudentApi;
import pe.com.edu.prismaapp.prisma.entities.*;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.StudentRepository;
import pe.com.edu.prismaapp.prisma.services.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {


    private final StudentRepository studentRepository;
    private final UserService userService;
    private final AreaService areaService;
    private final StageService stageService;
    private final StudentStageService studentStageService;
    private final StudentStageUserService studentStageUserService;

    public StudentServiceImpl(StudentRepository studentRepository, UserService userService,
                              AreaService areaService, StudentStageUserService studentStageUserService,
                              StudentStageService studentStageService, StageService stageService) {
        this.studentRepository = studentRepository;
        this.userService = userService;
        this.areaService = areaService;
        this.studentStageUserService = studentStageUserService;
        this.studentStageService = studentStageService;
        this.stageService = stageService;
    }

    @Override
    @Transactional
    public StudentApi.Response save(StudentApi.Create studentDTO) {
        var student = new Student();
        saveOrUpdate(student, studentDTO.name(), studentDTO.email(), studentDTO.phone(), studentDTO.dni(), studentDTO.areaId());
        studentRepository.saveAndFlush(student);

        Stage stage = null;
        if (studentDTO.stageId() == null) {
            Optional<Stage> optionalStage = stageService.getCurrentStage();

            if (optionalStage.isPresent()) {
                stage = optionalStage.get();
            }
        } else {
            stage = stageService.getStageById(studentDTO.stageId()).get();
        }
        var studentStage = studentStageService.saveStudent(student, stage, studentDTO.isActive());
        var studentStageUser = new StudentStageUser();
        studentStageUser.setStudentStage(studentStage);
        var tutor = userService.findTutorById(studentDTO.tutorId()).orElse(null);
        studentStageUser.setUser(tutor);
        studentStageUserService.save(studentStageUser);

        return StudentApi.Response.from(student, studentDTO.tutorId(), studentDTO.areaId(), stage.getId(), studentDTO.isActive());
    }

    @Override
    @Transactional
    public StudentApi.Response update(Long id, StudentApi.Update studentDTO) {
        var student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con ID: " + id));
        saveOrUpdate(student, studentDTO.name(), studentDTO.email(), studentDTO.phone(), studentDTO.dni(), studentDTO.areaId());
        studentRepository.save(student);

        var studentStage = studentStageService.updateStudent(studentDTO);
        var studentStageUser = studentStageUserService.findByStudentStageId(studentStage.getId());
        var tutor = userService.findTutorById(studentDTO.tutorId()).orElse(null);
        studentStageUser.setUser(tutor);
        studentStageUserService.save(studentStageUser);

        return StudentApi.Response.from(student, studentDTO.tutorId(), studentDTO.areaId(), studentDTO.stageId(), studentDTO.isActive());
    }

    private void saveOrUpdate(Student student, String name, String email, String phone, String dni, Long areaId) {
        student.setName(name);
        student.setEmail(email);
        student.setPhone(phone);
        student.setDni(dni);

        if (areaId != 0) {
            areaService.getAreaById(areaId).ifPresent(student::setArea);
        } else {
            student.setArea(null);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        //borrar de StudentStageUser
        studentStageUserService.deleteStudent(id);
        //borrar de StudentStage
        studentStageService.deleteStudent(id);
        //borrar de Student
        studentRepository.deleteById(id);
    }

    @Override
    public List<StudentApi.Response> findAll(Optional<Long> stageId) {
        List<Object[]> studentsList;

        //se saca el rol del token para validar la lista de datos a enviar
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = null;
        Long currentStage = null;
        if (stageId.isPresent()) {
            currentStage = stageId.get();
        }
        if (!currentUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            currentUserId = currentUser.getId();
        }
        studentsList = studentRepository.findStudentsByStage(currentStage, currentUserId);
        return studentsList.stream().map(student -> new StudentApi.Response(
                (Long) student[0],   // id
                (String) student[3], // name
                (String) student[2], // email
                (String) student[4], // phone
                (String) student[1], // dni
                (Long) student[6],   // tutorId
                (Long) student[5],   // areaId
                (Long) student[8],   // stageId
                (Boolean) student[7] // active

        )).toList();
    }

    @Override
    public boolean isStudentAssignedToTutor(Long studentId, Long tutorId) {
        Stage currentStage = stageService.getCurrentStage().orElse(null);
        if (currentStage == null) return false;
        StudentStage studentStage = studentStageService.getStudentStage(currentStage.getId(), studentId);
        return studentStageUserService.isStudentAssignedToTutor(studentStage.getId(), tutorId);
    }

    @Override
    public Student findByDniOrName(String dni, String name) {
        Student student = null;
        var findByName = false;
        if (dni != null && !dni.isEmpty()) {
            var students = studentRepository.findByDniIgnoreCase(dni);
            if (students.size() != 1) {
                findByName = true;
            }else{
                student = students.get(0);
            }
        }
        if (findByName) {
            student = studentRepository.findByNameIgnoreCase(name).orElse(null);
        }
        return student;
    }

    @Override
    @Transactional
    public void uploadStudents(MultipartFile file) {
        Optional<Stage> optionalStage = stageService.getCurrentStage();
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            DataFormatter dataFormatter = new DataFormatter();

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) { // Skip header row
                    continue;
                }
                String dni = dataFormatter.formatCellValue(row.getCell(0)).trim();
                String name = dataFormatter.formatCellValue(row.getCell(1)).trim();
                String area = dataFormatter.formatCellValue(row.getCell(2)).trim();
                String tutor = dataFormatter.formatCellValue(row.getCell(3)).trim();

                Student student = findByDniOrName(dni, name);

                Optional<Area> optionalArea = areaService.findAreaByName(area);
                Optional<User> optionalTutor = userService.findTutorByName(tutor);

                Long tutorId = optionalTutor.isPresent() ? optionalTutor.get().getId() : 0L;
                Long areaId = optionalArea.isPresent() ? optionalArea.get().getId() : 0L;
                Long stageId = optionalStage.isPresent() ? optionalStage.get().getId() : null;
                if (student == null) {
                    StudentApi.Create studentCreate = new StudentApi.Create(name, "", "", dni, tutorId, areaId, stageId, true);
                    this.save(studentCreate);
                } else {
                    StudentApi.Update studentUpdate = new StudentApi.Update(student.getId(), name, student.getEmail(), student.getPhone(), dni, tutorId, areaId, stageId, true);
                    this.update(student.getId(), studentUpdate);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Student findById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }
}
