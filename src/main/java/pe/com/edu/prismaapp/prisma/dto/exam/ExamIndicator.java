package pe.com.edu.prismaapp.prisma.dto.exam;

import lombok.Data;

import java.util.List;

@Data
public class ExamIndicator {
    private String indicator;
    private List<ExamResultIndicator> results;
}
