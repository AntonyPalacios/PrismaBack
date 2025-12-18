package pe.com.edu.prismaapp.prisma.dto.exam;

public record ExamEffectiveCourse(String name,
                                  long lectCorrect,
                                  long lectIncorrect,
                                  long nyoCorrect,
                                  long nyoIncorrect,
                                  long xCorrect,
                                  long xIncorrect,
                                  long geoCorrect,
                                  long geoIncorrect,
                                  long trigoCorrect,
                                  long trigoIncorrect,
                                  long estCorrect,
                                  long estIncorrect) {
}
