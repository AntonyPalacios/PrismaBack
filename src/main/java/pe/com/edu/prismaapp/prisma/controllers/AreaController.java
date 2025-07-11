package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prismaapp.prisma.services.AreaService;

@RestController
@RequestMapping("/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }


    @GetMapping
    ResponseEntity<Object> getAllAreas() {
        return ResponseEntity.ok(areaService.getAreas());
    }
}
