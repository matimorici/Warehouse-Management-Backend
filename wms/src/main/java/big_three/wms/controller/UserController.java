package big_three.wms.controller;

import big_three.wms.dto.UserCreateDTO;
import big_three.wms.model.User;
import big_three.wms.service.UserService;
import jakarta.validation.Valid;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

//    @Autowired
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
//    @PostMapping
//    public User crear(@RequestBody CreateUserDTO req) {
//        return userService.crear(req.getNombre(), req.getApellido(), req.getCuil(), req.getRol());
//    }
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody UserCreateDTO dto) {
        User newUser = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }
    @GetMapping
    public List<User> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User search(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        userService.deleteById(id);
    }
}