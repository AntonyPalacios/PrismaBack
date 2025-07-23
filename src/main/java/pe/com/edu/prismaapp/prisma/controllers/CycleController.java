package pe.com.edu.prismaapp.prisma.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.CycleDTO;
import pe.com.edu.prismaapp.prisma.services.CycleService;

@RestController
@RequestMapping("/cycles")
public class CycleController {

    private CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    @GetMapping
    ResponseEntity<Object> getAllCycles() {
        return ResponseEntity.ok(cycleService.findAll());
    }

    @GetMapping("/current")
    ResponseEntity<Object> getCurrentCycle() {
        return ResponseEntity.ok(cycleService.getCurrentCycle());
    }

    @PostMapping
    ResponseEntity<CycleDTO> createCycle(@RequestBody @Valid CycleDTO cycle) {
        CycleDTO c = cycleService.save(cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    ResponseEntity<CycleDTO> updateCycle(@PathVariable Long id,@RequestBody @Valid CycleDTO cycle) {
        CycleDTO c = cycleService.update(id,cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Object> deleteCycle(@PathVariable Long id) {
        cycleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
