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

    @GetMapping
    ResponseEntity<Object> getAllCycles() {
        return ResponseEntity.ok(cycleService.findAll());
    }

    @PostMapping
    ResponseEntity<CycleDTO> createCycle(@RequestBody CycleDTO cycle) {
        CycleDTO c = cycleService.save(cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    ResponseEntity<CycleDTO> updateCycle(@PathVariable Long id,@RequestBody CycleDTO cycle) {
        CycleDTO c = cycleService.update(id,cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Object> delteCycle(@PathVariable Long id) {
        boolean c = cycleService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }
}
