package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.entities.User;

public interface StudentApi {
    record Create(@NotBlank @Size(min = 2, max = 50) String name,
                  @Email String email,
                  @Size(max = 9) String phone,
                  @Size(max = 8) String dni,
                  Long tutorId,
                  Long areaId,
                  Long stageId,
                  @JsonProperty("isActive") boolean isActive) {
    }

    record Update(@NotBlank Long id,
                  @NotBlank @Size(min = 2, max = 50) String name,
                  @Email String email,
                  @Size(max = 9) String phone,
                  @Size(max = 8) String dni,
                  Long tutorId,
                  Long areaId,
                  Long stageId,
                  @JsonProperty("isActive") boolean isActive) {
    }

    record Response(@NotBlank Long id,
                    @NotBlank @Size(min = 2, max = 50) String name,
                    @Email String email,
                    @Size(max = 9) String phone,
                    @Size(max = 8) String dni,
                    Long tutorId,
                    Long areaId,
                    Long stageId,
                    @JsonProperty("isActive") boolean isActive) {
        public static StudentApi.Response from(Student student, Long tutorId, Long areaId, Long stageId, boolean isActive) {
            return new StudentApi.Response(
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    student.getPhone(),
                    student.getDni(),
                    tutorId,
                    areaId,
                    stageId,
                    isActive
            );
        }
    }
}
