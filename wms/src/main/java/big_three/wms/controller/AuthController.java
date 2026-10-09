package big_three.wms.controller;

import big_three.wms.dto.LoginRequestDTO;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.exception.InvalidCredentialsException;
import big_three.wms.exception.TooManyAttemptsException;
import big_three.wms.security.LoginAttemptService;
import big_three.wms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request, HttpServletResponse response) {
        if (loginAttemptService.isBlocked(dto.getCuil())){
            throw new TooManyAttemptsException("Demasiados intentos fallidos");
        }
        Authentication authRequest = new UsernamePasswordAuthenticationToken(dto.getCuil(), dto.getContrasena());
        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(authRequest);
        }catch(AuthenticationException authException) {
            loginAttemptService.recordFailure(dto.getCuil());
            throw new InvalidCredentialsException("CUIL o contraseña incorrectos");
        }
        loginAttemptService.recordSuccess(dto.getCuil());
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authResult);
        HttpSessionSecurityContextRepository sessionSecurity = new HttpSessionSecurityContextRepository();
        sessionSecurity.saveContext(context, request, response);
        UserResponseDTO responseDTO = userService.login(dto);
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, null);
        return ResponseEntity.noContent().build();
    }
}
