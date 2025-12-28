package pe.com.edu.prismaapp.prisma.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.GoalApi;
import pe.com.edu.prismaapp.prisma.services.GoalService;

@RestController
@RequestMapping("/goals")
@Tag(name = "Gestión de Metas", description = "Operaciones relacionadas a las metas individuales del alumno para un examen." +
        "Por defecto, todas las metas son de 10% del puntaje del examen anterior")
public class GoalController {
    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @Operation(
            summary = "Crear una meta",
            description = "Crea una meta para el examen de un alumno. Se debe ingresar el porcentaje que se desea incrementar"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PostMapping
    ResponseEntity<GoalApi.Response> createGoal(@RequestBody @Valid GoalApi.Create goal) {
        var c = goalService.save(goal);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Actualizar una meta",
            description = "Actualiza una meta para el examen de un alumno. Se debe ingresar el porcentaje que se desea incrementar"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PutMapping("/{id}")
    ResponseEntity<GoalApi.Response> updateGoal(@PathVariable Long id, @RequestBody @Valid GoalApi.Update goal) {
        var c = goalService.update(id, goal);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Eliminar una meta"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta eliminada correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
