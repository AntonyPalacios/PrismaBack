package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.*;
import pe.com.edu.prismaapp.prisma.repositories.StudentRepository;
import pe.com.edu.prismaapp.prisma.services.*;

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
        Student student = studentRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
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
    public List<StudentDTO> findAll(Optional<Long> stageId, Optional<Long> userId) {
        List<Object[]> studentsList;
        List<StudentDTO> studentDTOList = new ArrayList<>();
        Long currentUser = null;
        Long currentStage = null;
        if(stageId.isPresent()){
            currentStage = stageId.get();
        }
        if(userId.isPresent()){
            currentUser = userId.get();
        }
        studentsList = studentRepository.findStudentsByStage(currentStage,currentUser);
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
}
