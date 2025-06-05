package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.CycleDTO;
import pe.com.edu.prismaapp.prisma.services.CycleService;

@RestController
@RequestMapping("/cycle")
public class CycleController {

    private CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    @PostMapping
    ResponseEntity<CycleDTO> createCycle(@RequestBody CycleDTO cycle) {
        CycleDTO c = cycleService.save(cycle);
        return ResponseEntity.status(HttpStatus.CREATED).body(c);
    }

    @GetMapping
    ResponseEntity<Object> getAllCycles() {
        return ResponseEntity.ok(cycleService.findAll());
    }
}
