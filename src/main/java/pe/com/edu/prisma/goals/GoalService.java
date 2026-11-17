package pe.com.edu.prisma.goals;

import java.util.List;
import pe.com.edu.prisma.goals.dto.GoalMockExamDto;
import pe.com.edu.prisma.goals.dto.GoalSheetDto;
import pe.com.edu.prisma.goals.dto.SaveGoalSheetRequest;
import pe.com.edu.prisma.goals.dto.StudentGoalDto;

/**
 * Goals of correct answers per area (REQ-GOALS-01, agreements of 2026-11-17):
 * set for a whole group per mock exam. A TUTOR works with the students
 * assigned to them today; an ADMIN with anyone.
 */
public interface GoalService {

    /** The mock exams of the active cycle, by date. */
    List<GoalMockExamDto> mockExams();

    /** A TUTOR always gets their own group (403 with a tutorId); an ADMIN everyone, or one tutor's group. */
    GoalSheetDto sheet(Long mockExamId, Long tutorId);

    /** Saves the goals sent, all or nothing; a null target removes the goal. */
    void save(SaveGoalSheetRequest request);

    /** The student's goals in the active cycle: ADMIN, or their current tutor. */
    List<StudentGoalDto> findByStudent(Long studentId);
}
