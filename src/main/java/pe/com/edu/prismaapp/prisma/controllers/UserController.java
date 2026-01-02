package pe.com.edu.prismaapp.prisma.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.edu.prismaapp.prisma.dto.UserApi;
import pe.com.edu.prismaapp.prisma.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Gestión de Usuarios", description = "Operaciones relacionadas a los usuarios del sistema (Admin/tutor)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Listar usuarios"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios listados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @GetMapping
    ResponseEntity<List<UserApi.Response>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(
            summary = "Obtener usuario actual",
            description = "Obtiene el usuario actual según el token de la petición"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @GetMapping("/current")
    ResponseEntity<UserApi.CurrentUser> getCurrentUser() {
        var c = userService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Crear un usuario",
            description = "Crea un usuario con sus roles"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserApi.Response> createUser(@RequestBody UserApi.Create userDTO) {
        var c = userService.save(userDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Actualiza un usuario",
            description = "Actualiza los datos y roles de un usuario"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Usuario no existe o datos ingresados de forma incorrecta", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserApi.Response> updateUser(@PathVariable Long id, @RequestBody UserApi.Update userDTO) {
        var c = userService.update(id, userDTO);
        return ResponseEntity.status(HttpStatus.OK).body(c);
    }

    @Operation(
            summary = "Eliminar un usuario",
            description = "Elimina un usuario y todos sus datos asociados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "Usuario no existe", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del sistema", content = @Content),
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
