package pe.com.edu.prismaapp.prisma.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.UserDTO;
import pe.com.edu.prismaapp.prisma.services.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    ResponseEntity<Object> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/current")
    ResponseEntity<Object> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PostMapping
    ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO c = userService.save(userDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @PutMapping("/{id}")
    ResponseEntity<UserDTO> updateUser(@PathVariable Long id,@RequestBody UserDTO userDTO) {
        UserDTO c = userService.update(id,userDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Object> deleteUser(@PathVariable Long id) {
        Long idUser = userService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(idUser);
    }
}
