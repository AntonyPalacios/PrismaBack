package pe.com.edu.prismaapp.prisma.dto.exam;

public record ExamScore(String name,
                        double score,
                        int merit,
                        double min,
                        double max,
                        double avg) {
}
