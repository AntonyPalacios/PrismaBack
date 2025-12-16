package pe.com.edu.prismaapp.prisma.dto.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExamCourseResultDTO {
    private String name;
    private int lectCorrect;
    private int lectIncorrect;
    private int nyoCorrect;
    private int nyoIncorrect;
    @JsonProperty("xCorrect")
    private int xCorrect;
    @JsonProperty("xIncorrect")
    private int xIncorrect;
    private int geoCorrect;
    private int geoIncorrect;
    private int trigoCorrect;
    private int trigoIncorrect;
    private int estCorrect;
    private int estIncorrect;
}
