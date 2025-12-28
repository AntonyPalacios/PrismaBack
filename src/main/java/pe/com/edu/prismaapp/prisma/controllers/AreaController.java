package pe.com.edu.prismaapp.prisma.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prismaapp.prisma.dto.AreaApi;
import pe.com.edu.prismaapp.prisma.services.AreaService;

import java.util.List;

@RestController
@RequestMapping("/areas")
@Tag(name = "Gestión de Areas", description = "Lista la areas existentes")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @Operation(
            summary = "Obtener todas las áreas",
            description = "Lista todas las áreas a las que puede pertenecer un alumno"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Áreas listadas correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @GetMapping
    ResponseEntity<List<AreaApi.Response>> getAllAreas() {
        return ResponseEntity.ok(areaService.getAreas());
    }
}
