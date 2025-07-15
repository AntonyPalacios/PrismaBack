package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.StageDTO;
import pe.com.edu.prismaapp.prisma.services.StageService;

import java.util.Optional;

@RestController
@RequestMapping("/stage")
public class StageController {

    private StageService stageService;

    public StageController(StageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    ResponseEntity<Object> getAllStages(@RequestParam Long idCycle) {
        return ResponseEntity.ok(stageService.findAll(idCycle));
    }

    @GetMapping("/current")
    ResponseEntity<Object> getCurrentStage() {

        return ResponseEntity.ok(stageService.getCurrentStageDTO());
    }

    @PostMapping
    ResponseEntity<Object> createStage(@RequestBody StageDTO stage) {
        StageDTO c = stageService.save(stage);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    ResponseEntity<Object> updateStage(@PathVariable Long id,@RequestBody StageDTO stageDTO) {
        StageDTO c = stageService.update(id,stageDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Object> deleteStage(@PathVariable Long id) {
        boolean c = stageService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }
}
