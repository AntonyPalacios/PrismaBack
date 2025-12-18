package pe.com.edu.prismaapp.prisma.dto.exam;

public record ExamSectionSummary(
        long examId,
        int minCorrect,
        int minIncorrect,
        int maxCorrect,
        int maxIncorrect,
        double avgCorrect,
        double avgIncorrect
) {
}
