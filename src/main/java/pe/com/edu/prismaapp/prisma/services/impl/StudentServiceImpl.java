package pe.com.edu.prismaapp.prisma.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.auth.CustomUserDetails;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.*;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.StudentRepository;
import pe.com.edu.prismaapp.prisma.services.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
    public StudentDTO save(StudentDTO studentDTO) {
        Student student = new Student();
        mapStudentValues(studentDTO, student);

        Optional<Stage> optionalStage = stageService.getCurrentStage();
        Stage stage = null;
        if(optionalStage.isPresent()){
            stage = optionalStage.get();
        }
        StudentStage studentStage = studentStageService.saveStudent(student,stage,studentDTO.isActive());
        StudentStageUser studentStageUser = new StudentStageUser();
        studentStageUser.setStudentStage(studentStage);
        User tutor = userService.findTutorById(studentDTO.getTutorId()).orElse(null);
        studentStageUser.setUser(tutor);
        studentStageUserService.save(studentStageUser);


        studentDTO.setId(student.getId());
        return studentDTO;
    }

    @Override
    @Transactional
    public StudentDTO update(Long id, StudentDTO studentDTO) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con ID: " + id));
        mapStudentValues(studentDTO, student);

        StudentStage studentStage = studentStageService.updateStudent(student,studentDTO);
        StudentStageUser studentStageUser = studentStageUserService.findByStudentStageId(studentStage.getId());
        User tutor = userService.findTutorById(studentDTO.getTutorId()).orElse(null);
        studentStageUser.setUser(tutor);
        studentStageUserService.save(studentStageUser);

        return studentDTO;
    }

    private void mapStudentValues(StudentDTO studentDTO, Student student) {
        student.setName(studentDTO.getName());
        student.setEmail(studentDTO.getEmail());
        student.setPhone(studentDTO.getPhone());
        student.setDni(studentDTO.getDni());

        if(studentDTO.getAreaId() != 0){
            areaService.getAreaById(studentDTO.getAreaId()).ifPresent(student::setArea);
        }else {
            student.setArea(null);
        }
        studentRepository.save(student);
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
    public List<StudentDTO> findAll(Optional<Long> stageId) {
        List<Object[]> studentsList;
        List<StudentDTO> studentDTOList = new ArrayList<>();

        //se saca el rol del token para validar la lista de datos a enviar
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = null;
        Long currentStage = null;
        if(stageId.isPresent()){
            currentStage = stageId.get();
        }
        if(!currentUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))){
            currentUserId = currentUser.getId();
        }
        studentsList = studentRepository.findStudentsByStage(currentStage,currentUserId);
        for (Object[] student : studentsList) {
            StudentDTO studentDTO = new StudentDTO();
            studentDTO.setId((Long) student[0]);
            studentDTO.setDni((String) student[1]);
            studentDTO.setEmail((String) student[2]);
            studentDTO.setName((String) student[3]);
            studentDTO.setPhone((String) student[4]);
            studentDTO.setAreaId((Long) student[5]);
            studentDTO.setTutorId((Long) student[6]);
            studentDTO.setActive((Boolean) student[7]);
            studentDTO.setStageId((Long) student[8]);
            studentDTOList.add(studentDTO);
        }


        return studentDTOList;
    }

    @Override
    public boolean isStudentAssignedToTutor(Long studentId, Long tutorId) {
        Stage currentStage = stageService.getCurrentStage().orElse(null);
        if(currentStage == null) return false;
        StudentStage studentStage = studentStageService.getStudentFromCurrentStage(currentStage.getId(), studentId);
        return studentStageUserService.isStudentAssignedToTutor(studentStage.getId(),tutorId);
    }

    @Override
    @Transactional
    public void uploadStudents(MultipartFile file) {
        Optional<Stage> optionalStage = stageService.getCurrentStage();
        Stage stage;

        try(InputStream inputStream = file.getInputStream()){
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

                Student student = null;
                if(!dni.isEmpty()){
                    student = studentRepository.findByDniIgnoreCase(dni).orElse(null);
                }
                if(student == null){
                    student = studentRepository.findByNameIgnoreCase(name).orElse(null);
                }

                StudentDTO studentDTO = new StudentDTO();
                studentDTO.setDni(dni);
                studentDTO.setName(name);

                Optional<Area> optionalArea = areaService.findAreaByName(area);
                Optional<User> optionalTutor = userService.findTutorByName(tutor);

                if (optionalTutor.isPresent()) {
                    studentDTO.setTutorId(optionalTutor.get().getId());
                }else{
                    studentDTO.setTutorId(0L);
                }

                if (optionalArea.isPresent()) {
                    studentDTO.setAreaId(optionalArea.get().getId());
                }else{
                    studentDTO.setAreaId(0L);
                }

                studentDTO.setActive(true);
                if(student == null){
                    studentDTO.setEmail("");
                    studentDTO.setPhone("");
                    this.save(studentDTO);
                }else {

                    if(optionalStage.isPresent()){
                        stage = optionalStage.get();
                        studentDTO.setStageId(stage.getId());
                    }
                    studentDTO.setEmail(student.getEmail());
                    studentDTO.setPhone(student.getPhone());
                    this.update(student.getId(), studentDTO);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
