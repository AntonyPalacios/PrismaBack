package pe.com.edu.prismaapp.prisma.dto.exam;

import java.util.List;

public record ExamDataSummary (
        List<ExamData> examData,
        List<ExamSectionSummary> lectData,
        List<ExamSectionSummary> mateData) {
}
