package pe.com.edu.prismaapp.prisma.dto.exam;

import lombok.Data;

import java.util.List;

@Data
public class ExamDataSummary {
    private List<ExamData> examData;
    private List<ExamIndicator> lectData;
    private List<ExamIndicator> mateData;
}
