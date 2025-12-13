package pe.com.edu.prismaapp.prisma.controllers;

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
public class CycleController {

    private final CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    @GetMapping
    ResponseEntity<List<CycleApi.Response>> getAllCycles() {
        return ResponseEntity.ok(cycleService.findAll());
    }

    @GetMapping("/current")
    ResponseEntity<CycleApi.Response> getCurrentCycle() {
        return ResponseEntity.ok(cycleService.getCurrentCycle());
    }

    @PostMapping
    ResponseEntity<CycleApi.Response> createCycle(@RequestBody @Valid CycleApi.Create cycle) {
        var c = cycleService.save(cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    ResponseEntity<CycleApi.Response> updateCycle(@PathVariable Long id, @RequestBody @Valid CycleApi.Update cycle) {
        var c = cycleService.update(id, cycle);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        cycleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
