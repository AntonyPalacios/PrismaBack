package pe.com.edu.prismaapp.prisma.dto.exam;

public record ExamResultRecord(Long examId,
                               Long areaId,
                               String name,
                               int merit,
                               double totalScore){}