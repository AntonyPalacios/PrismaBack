package pe.com.edu.prismaapp.prisma.controllers;

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
public class StageController {

    private final StageService stageService;

    public StageController(StageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    ResponseEntity<List<StageApi.Response>> getAllStages(@RequestParam Long idCycle) {
        return ResponseEntity.ok(stageService.findAll(idCycle));
    }

    @GetMapping("/current")
    ResponseEntity<StageApi.Response> getCurrentStage() {
        return ResponseEntity.ok(stageService.getCurrentStageDTO());
    }

    @PostMapping
    ResponseEntity<StageApi.Response> createStage(@RequestBody @Valid StageApi.Create stage) {
        var s = stageService.save(stage);
        return ResponseEntity.status(HttpStatus.OK).body(s);
    }

    @PutMapping("/{id}")
    ResponseEntity<StageApi.Response> updateStage(@PathVariable Long id,@RequestBody @Valid StageApi.Update stageDTO) {
        var c = stageService.update(id,stageDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteStage(@PathVariable Long id) {
        stageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
