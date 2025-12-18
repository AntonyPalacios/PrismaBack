package pe.com.edu.prismaapp.prisma.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public record ExamData(
        Long id,
        String name,
        @JsonFormat(pattern="dd/MM/yyyy") Date date
) {}
