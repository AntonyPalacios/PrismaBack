package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class ExamScoreDTO {
    private String name;
    private double score;
    private double min;
    private double max;
    private double avg;
    private int merit;
    private int totalCorrect;
    private int totalIncorrect;
    private int totalLectCorrect;
    private int totalLectIncorrect;
    private int totalMateCorrect;
    private int totalMateIncorrect;

    public ExamScoreDTO(String name, long totalCorrect, long totalIncorrect) {
        this.name = name;
        this.totalCorrect = (int) totalCorrect;
        this.totalIncorrect = (int) totalIncorrect;
    }
}
