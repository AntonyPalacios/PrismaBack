package pe.com.edu.prismaapp.prisma.dto;

import lombok.Data;

@Data
public class ExamResultIndicator {
    private Long examId;
    private int correct;
    private int incorrect;
}
