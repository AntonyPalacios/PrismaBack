package pe.com.edu.prismaapp.prisma.services;

import jakarta.validation.Valid;
import pe.com.edu.prismaapp.prisma.dto.ExamDTO;

import java.util.List;

public interface ExamService {
    List<ExamDTO> getExams(Long cycleId,Long stageId);

    ExamDTO save(@Valid ExamDTO exam);

    ExamDTO update(Long id, ExamDTO examDTO);
}
