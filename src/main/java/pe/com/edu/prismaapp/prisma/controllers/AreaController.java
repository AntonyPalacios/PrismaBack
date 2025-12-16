package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prismaapp.prisma.dto.AreaApi;
import pe.com.edu.prismaapp.prisma.services.AreaService;

import java.util.List;

@RestController
@RequestMapping("/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }


    @GetMapping
    ResponseEntity<List<AreaApi.Response>> getAllAreas() {
        return ResponseEntity.ok(areaService.getAreas());
    }
}
