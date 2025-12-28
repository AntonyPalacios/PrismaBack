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
import pe.com.edu.prismaapp.prisma.dto.CycleApi;
import pe.com.edu.prismaapp.prisma.services.CycleService;

import java.util.List;

@RestController
@RequestMapping("/cycles")
@Tag(name = "Gestión de Ciclos", description = "Operaciones relacionadas a los Ciclos")
public class CycleController {

    private final CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    @Operation(
            summary = "Obtener todos los ciclos",
            description = "Lista todos los ciclos ordenado por el más reciente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciclos listados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @GetMapping
    ResponseEntity<List<CycleApi.Response>> getAllCycles() {
        return ResponseEntity.ok(cycleService.findAll());
    }

    @Operation(
            summary = "Obtener el ciclo actual",
            description = "Obtiene el ciclo actual según la fecha. Si no existe, devuelve el último ciclo activo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciclo encontrado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe ningún ciclo. Debe crear uno", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @GetMapping("/current")
    ResponseEntity<CycleApi.Response> getCurrentCycle() {
        return ResponseEntity.ok(cycleService.getCurrentCycle());
    }

    @Operation(
            summary = "Crear un ciclo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciclo creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PostMapping
    ResponseEntity<CycleApi.Response> createCycle(@RequestBody @Valid CycleApi.Create cycle) {
        var c = cycleService.save(cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Actualizar un ciclo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciclo actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PutMapping("/{id}")
    ResponseEntity<CycleApi.Response> updateCycle(@PathVariable Long id, @RequestBody @Valid CycleApi.Update cycle) {
        var c = cycleService.update(id, cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Eliminar un ciclo",
            description = "Elimina un ciclo y todo lo asociado (etapas, exámenes, alumnos)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ciclo eliminado correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        cycleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
