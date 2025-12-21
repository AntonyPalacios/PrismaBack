package pe.com.edu.prismaapp.prisma.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.GoalApi;
import pe.com.edu.prismaapp.prisma.services.GoalService;

@RestController
@RequestMapping("/goals")
public class GoalController {
    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    ResponseEntity<GoalApi.Response> createGoal(@RequestBody @Valid GoalApi.Create goal) {
        var c = goalService.save(goal);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    ResponseEntity<GoalApi.Response> updateGoal(@PathVariable Long id, @RequestBody @Valid GoalApi.Update goal) {
        var c = goalService.update(id, goal);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
