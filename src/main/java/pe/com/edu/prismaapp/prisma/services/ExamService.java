package pe.com.edu.prismaapp.prisma.services;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.ExamDTO;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;

import java.io.IOException;
import java.util.List;

public interface ExamService {
    List<ExamDTO> getExams(Long cycleId,Long stageId);

    ExamDTO save(@Valid ExamDTO exam);

    ExamDTO update(Long id, ExamDTO examDTO);

    void importResults(Long examId, AreaEnum area, MultipartFile file) throws IOException;
}
