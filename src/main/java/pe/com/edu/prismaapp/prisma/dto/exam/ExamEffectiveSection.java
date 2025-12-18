package pe.com.edu.prismaapp.prisma.dto.exam;

public record ExamEffectiveSection(
        String name,
        long lectCorrect,
        long lectIncorrect,
        long mateCorrect,
        long mateIncorrect
){}
