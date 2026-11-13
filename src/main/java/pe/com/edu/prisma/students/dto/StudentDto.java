package pe.com.edu.prisma.students.dto;

import java.time.LocalDate;

public record StudentDto(
        Long id,
        String firstName,
        String lastNamePat,
        String lastNameMat,
        String dni,
        String email,
        String phone,
        LocalDate birthDate,
        boolean active
) {
}
