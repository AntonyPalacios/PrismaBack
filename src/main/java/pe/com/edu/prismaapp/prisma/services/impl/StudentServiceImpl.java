package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;
import pe.com.edu.prismaapp.prisma.entities.StudentStageUser;
import pe.com.edu.prismaapp.prisma.repositories.StudentRepository;
import pe.com.edu.prismaapp.prisma.repositories.StudentStageUserRepository;
import pe.com.edu.prismaapp.prisma.services.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {


    private final StudentRepository studentRepository;
    private final UserService userService;
    private final AreaService areaService;
    private final StudentStageService studentStageService;
    private final StudentStageUserRepository studentStageUserRepository;

    public StudentServiceImpl(StudentRepository studentRepository, UserService userService,
                              AreaService areaService, StudentStageService stageService,
                              StudentStageUserRepository studentStageUserRepository) {
        this.studentRepository = studentRepository;
        this.userService = userService;
        this.areaService = areaService;
        this.studentStageService = stageService;
        this.studentStageUserRepository = studentStageUserRepository;
    }

    @Override
    public StudentDTO save(StudentDTO studentDTO) {
        Student student = new Student();
        student.setName(studentDTO.getName());
        student.setEmail(studentDTO.getEmail());
        student.setPhone(studentDTO.getPhone());
        student.setDni(studentDTO.getDni());

        //buscar id del tutor
        if(studentDTO.getTutorId()>0){
            userService.findTutorById(studentDTO.getTutorId()).ifPresent(student::setTutor);
        }

        areaService.getAreaById(studentDTO.getAreaId()).ifPresent(student::setArea);
        studentRepository.save(student);
        StudentStage studentStage = studentStageService.saveStudent(student,studentDTO.isActive());
        StudentStageUser studentStageUser = new StudentStageUser();
        studentStageUser.setStudentStage(studentStage);
        studentStageUser.setUser(student.getTutor());
        studentStageUserRepository.save(studentStageUser);


        studentDTO.setId(student.getId());
        return studentDTO;
    }

    @Override
    public StudentDTO update(Long id, StudentDTO studentDTO) {
        Student student = studentRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        student.setName(studentDTO.getName());
        student.setEmail(studentDTO.getEmail());
        student.setPhone(studentDTO.getPhone());
        student.setDni(studentDTO.getDni());

        //buscar id del tutor
        if(studentDTO.getTutorId()>0){
            userService.findTutorById(studentDTO.getTutorId()).ifPresent(student::setTutor);
        }else{
            student.setTutor(null);
        }

        areaService.getAreaById(studentDTO.getAreaId()).ifPresent(student::setArea);
        studentRepository.save(student);
        StudentStage studentStage = studentStageService.updateStudent(student,studentDTO);
        StudentStageUser studentStageUser = studentStageUserRepository.findByStudentStage_Id(studentStage.getId());
        studentStageUser.setUser(student.getTutor());
        studentStageUserRepository.save(studentStageUser);

        return studentDTO;
    }

    @Override
    public List<StudentDTO> findAll(Long stageId, Optional<Long> userId) {
        List<Object[]> studentsList;
        List<StudentDTO> studentDTOList = new ArrayList<>();
        Long currentUser = null;
        if(userId.isPresent()){
            currentUser = userId.get();
        }
        studentsList = studentRepository.findStudentsByStage(stageId,currentUser);
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
