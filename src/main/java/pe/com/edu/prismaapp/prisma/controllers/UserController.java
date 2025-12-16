package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.UserApi;
import pe.com.edu.prismaapp.prisma.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    ResponseEntity<List<UserApi.Response>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/current")
    ResponseEntity<UserApi.CurrentUser> getCurrentUser() {
        var c = userService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserApi.Response> createUser(@RequestBody UserApi.Create userDTO) {
        var c = userService.save(userDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserApi.Response> updateUser(@PathVariable Long id, @RequestBody UserApi.Update userDTO) {
        var c = userService.update(id, userDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
