package big_three.wms.controller;

import big_three.wms.model.User;
import big_three.wms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> listar() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User buscar(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PostMapping
    public User crear(@RequestBody CrearUserRequest req) {
        return userService.crear(req.getNombre(), req.getApellido(), req.getCuil(), req.getRol());
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        userService.deleteById(id);
    }
}