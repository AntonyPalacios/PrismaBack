package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Student;

public interface StudentApi {

    @Schema(name = "CreateStudentRequest", description = "Datos requeridos para crear un alumno")
    record Create(
            @NotBlank
            @Size(min = 2, max = 50)
            @Schema(description = "Nombre del alumno", example = "Silvia Valle")
            String name,

            @Email
            @Schema(description = "Correo del alumno", example = "alumno2@gmail.com")
            String email,

            @Size(max = 9)
            @Schema(description = "Celular del alumno", example = "987654321")
            String phone,

            @Size(max = 8)
            @Schema(description = "DNI del alumno", example = "75398622")
            String dni,

            @Schema(description = "Id del tutor al que esta asignado", example = "2")
            Long tutorId,

            @Schema(description = "Id del área al que pertenece", example = "2")
            Long areaId,

            @Schema(description = "Id de la etapa a la que pertenece", example = "2")
            Long stageId,

            @JsonProperty("isActive")
            @Schema(description = "Flag de estado", example = "true")
            boolean isActive
    ) {}

    @Schema(name = "UpdateStudentRequest", description = "Datos requeridos para actualizar un alumno")
    record Update(
            @NotNull
            @Schema(description = "Id del alumno", example = "100")
            Long id,

            @NotBlank
            @Size(min = 2, max = 50)
            @Schema(description = "Nombre del alumno", example = "Silvia Valle")
            String name,

            @Email
            @Schema(description = "Correo del alumno", example = "alumno2@gmail.com")
            String email,

            @Size(max = 9)
            @Schema(description = "Celular del alumno", example = "987654321")
            String phone,

            @Size(max = 8)
            @Schema(description = "DNI del alumno", example = "75398622")
            String dni,

            @Schema(description = "Id del tutor al que esta asignado", example = "2")
            Long tutorId,

            @Schema(description = "Id del área al que pertenece", example = "2")
            Long areaId,

            @Schema(description = "Id de la etapa a la que pertenece", example = "2")
            Long stageId,

            @JsonProperty("isActive")
            @Schema(description = "Flag de estado", example = "true")
            boolean isActive
    ) {}

    @Schema(name = "StudentResponse", description = "Respuesta con los detalles del alumno")
    record Response(
            @NotNull
            @Schema(description = "Id del alumno", example = "100")
            Long id,

            @NotBlank
            @Size(min = 2, max = 50)
            @Schema(description = "Nombre del alumno", example = "Silvia Valle")
            String name,

            @Email
            @Schema(description = "Correo del alumno", example = "alumno2@gmail.com")
            String email,

            @Size(max = 9)
            @Schema(description = "Celular del alumno", example = "987654321")
            String phone,

            @Size(max = 8)
            @Schema(description = "DNI del alumno", example = "75398622")
            String dni,

            @Schema(description = "Id del tutor al que esta asignado", example = "2")
            Long tutorId,

            @Schema(description = "Id del área al que pertenece", example = "2")
            Long areaId,

            @Schema(description = "Id de la etapa a la que pertenece", example = "2")
            Long stageId,

            @JsonProperty("isActive")
            @Schema(description = "Flag de estado", example = "true")
            boolean isActive
    ) {
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
