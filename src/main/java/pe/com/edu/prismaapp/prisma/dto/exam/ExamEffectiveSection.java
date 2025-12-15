package pe.com.edu.prismaapp.prisma.dto.exam;

public record ExamEffectiveSection(
        String name,
        Long totalCorrect,
        Long totalIncorrect
){}
