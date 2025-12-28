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
import pe.com.edu.prismaapp.prisma.dto.StageApi;
import pe.com.edu.prismaapp.prisma.services.StageService;

import java.util.List;

@RestController
@RequestMapping("/stages")
@Tag(name = "Gestión de Etapas", description = "Operaciones relaciondas a las etapas de un ciclo")
public class StageController {

    private final StageService stageService;

    public StageController(StageService stageService) {
        this.stageService = stageService;
    }

    @Operation(
            summary = "Listar etapas de un ciclo",
            description = "Lista todas las etapas de un ciclo ordenadas por más reciente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Etapas listadas correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping
    ResponseEntity<List<StageApi.Response>> getAllStages(@RequestParam Long idCycle) {
        return ResponseEntity.ok(stageService.findAll(idCycle));
    }

    @Operation(
            summary = "Obtener etapa actual",
            description = "Obtiene la etapa actual según la fecha. Debe existir un ciclo actual"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Etapa encontrada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe ninguna etapa. Debe crear una", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @GetMapping("/current")
    ResponseEntity<StageApi.Response> getCurrentStage() {
        return ResponseEntity.ok(stageService.getCurrentStageDTO());
    }

    @Operation(
            summary = "Crear una etapa",
            description = "Crea una etapa para el ciclo dado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Etapa creada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe el ciclo al que se quiere crear la etapa", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @PostMapping
    ResponseEntity<StageApi.Response> createStage(@RequestBody @Valid StageApi.Create stage) {
        var s = stageService.save(stage);
        return ResponseEntity.status(HttpStatus.OK).body(s);
    }

    @Operation(
            summary = "Actualizar una etapa",
            description = "Actualiza una etapa"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Etapa actualizada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe el ciclo o la etapa que se quiere actualizar", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @PutMapping("/{id}")
    ResponseEntity<StageApi.Response> updateStage(@PathVariable Long id,@RequestBody @Valid StageApi.Update stageDTO) {
        var c = stageService.update(id,stageDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Eliminar una etapa"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Etapa eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe la etapa a eliminar", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteStage(@PathVariable Long id) {
        stageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
