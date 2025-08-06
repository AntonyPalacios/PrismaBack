package pe.com.edu.prismaapp.prisma.dto;

import lombok.Data;

@Data
public class ExamResultDTO {
    private Long examId;
    private Long areaId;
    private String name;
    private int merit;
    private Double totalScore;

    public ExamResultDTO(Long examId, Long areaId, String name, int merit, Double totalScore) {
        this.examId = examId;
        this.areaId = areaId;
        this.name = name;
        this.merit = merit;
        this.totalScore = totalScore;
    }
}
