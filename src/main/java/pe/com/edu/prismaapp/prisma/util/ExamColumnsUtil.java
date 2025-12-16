package pe.com.edu.prismaapp.prisma.util;

public interface ExamColumnsUtil {
    record AreaConfig(int goodCol, int badCol, int scoreCol, int tutorCol) {}

    record CourseColumns(int lectCorrect, int lectIncorrect, int lectBlank,
                         int nyoCorrect, int nyoIncorrect, int nyoBlank,
                         int xCorrect, int xIncorrect, int xBlank,
                         int geoCorrect, int geoIncorrect, int geoBlank,
                         int trigoCorrect, int trigoIncorrect, int trigoBlank,
                         int estCorrect, int estIncorrect, int estBlank
    ) {
    }
}
