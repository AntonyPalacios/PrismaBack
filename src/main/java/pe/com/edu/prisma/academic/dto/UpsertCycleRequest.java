package pe.com.edu.prisma.academic.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Create or edit a cycle: its name and dates. The status is never set from here. */
public record UpsertCycleRequest(

        @NotBlank(message = "El ciclo necesita un nombre.")
        @Size(max = 150, message = "El nombre del ciclo es demasiado largo.")
        String name,

        @NotNull(message = "Ingresa la fecha de inicio del ciclo.")
        LocalDate startDate,

        // optional: a cycle without end date is open-ended
        LocalDate endDate
) {

    @JsonIgnore
    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio.")
    public boolean isEndDateValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
