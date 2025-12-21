package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.GoalApi;
import pe.com.edu.prismaapp.prisma.entities.Exam;
import pe.com.edu.prismaapp.prisma.entities.Goal;
import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.repositories.GoalRepository;
import pe.com.edu.prismaapp.prisma.services.ExamService;
import pe.com.edu.prismaapp.prisma.services.GoalService;
import pe.com.edu.prismaapp.prisma.services.StudentService;

@Service
public class GoalServiceImpl implements GoalService {

    private final ExamService examService;
    private final StudentService studentService;
    private final GoalRepository goalRepository;

    public GoalServiceImpl(ExamService examService, StudentService studentService, GoalRepository goalRepository) {
        this.examService = examService;
        this.studentService = studentService;
        this.goalRepository = goalRepository;
    }

    @Override
    public GoalApi.Response save(GoalApi.Create goalCreate) {
        //obtener examen
        Exam exam = examService.findById(goalCreate.examId()).orElseThrow();
        //obtener alumno
        Student student = studentService.findById(goalCreate.studentId()).orElseThrow();
        //guardar
        Goal goal = new Goal();
        goal.setExam(exam);
        goal.setStudent(student);
        goal.setScoreGoal(goalCreate.scoreGoal());
        goalRepository.save(goal);
        return GoalApi.Response.from(goal);
    }

    @Override
    public GoalApi.Response update(Long id, GoalApi.Update goalUpdate) {
        Goal goal = goalRepository.findById(id).orElseThrow();
        goal.setScoreGoal(goalUpdate.scoreGoal());
        goalRepository.save(goal);
        return GoalApi.Response.from(goal);
    }

    @Override
    public void delete(Long id) {
        Goal goal = goalRepository.findById(id).orElseThrow();
        goalRepository.delete(goal);
    }
}
