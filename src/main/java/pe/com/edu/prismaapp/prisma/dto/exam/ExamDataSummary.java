package pe.com.edu.prismaapp.prisma.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "NotesTalkResponse", description = "Respuesta con los datos necesarios para armar la charla de notas de un tutor")
public record ExamDataSummary (
        List<ExamData> examData,
        List<ExamSectionSummary> lectData,
        List<ExamSectionSummary> mateData) {
}
