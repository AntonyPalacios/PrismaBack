package pe.com.edu.prismaapp.prisma.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamIndicator {
    private String indicator;
    private List<ExamResultIndicator> results;
}
