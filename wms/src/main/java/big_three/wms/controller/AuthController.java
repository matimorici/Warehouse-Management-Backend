package big_three.wms.controller;

import big_three.wms.dto.LoginRequestDTO;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        UserResponseDTO response = userService.login(dto);
        return ResponseEntity.ok(response);
    }
}
