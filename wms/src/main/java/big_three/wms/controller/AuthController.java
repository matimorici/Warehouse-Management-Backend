package big_three.wms.controller;

import big_three.wms.dto.LoginRequestDTO;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request, HttpServletResponse response) {
        Authentication authRequest = new UsernamePasswordAuthenticationToken(dto.getCuil(), dto.getContrasena());
        Authentication authResult = authenticationManager.authenticate(authRequest);
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authResult);
        HttpSessionSecurityContextRepository sessionSecurity = new HttpSessionSecurityContextRepository();
        sessionSecurity.saveContext(context, request, response);
        UserResponseDTO responseDTO = userService.login(dto);
        return ResponseEntity.ok(responseDTO);
    }
}
