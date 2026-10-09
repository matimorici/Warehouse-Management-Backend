package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.exception.InvalidCredentialsException;
import big_three.wms.model.Role;
import big_three.wms.security.LoginAttemptService;
import big_three.wms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private static final String BODY_VALID_CREDENTIALS = """
            {"cuil": "20-12345678-9", "contrasena": "Password1"}
            """;

    private static final String BODY_INVALID_PASSWORD = """
            {"cuil": "20-12345678-9", "contrasena": "Password2"}
            """;

    private static final String BODY_INVALID_CUIL = """
            {"cuil": "20-12345678-0", "contrasena": "Password1"}
            """;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("20-12345678-9")
                .password(passwordEncoder.encode("Password1"))
                .roles("OPERARIO")
                .build();

        when(userDetailsService.loadUserByUsername("20-12345678-9")).thenReturn(userDetails);
    }

    @Test
    void login_validCredentials_returns200() throws Exception {
        when(userService.login(any())).thenReturn(new UserResponseDTO(1L, "Juan", "Perez", "20-12345678-9", Role.OPERARIO));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALID_CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.cuil").value("20-12345678-9"));
    }

    @Test
    void login_invalidCredentials_returns401WithError() throws Exception {
        when(userService.login(any())).thenThrow(new InvalidCredentialsException("CUIL o contraseña incorrectos"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALID_CREDENTIALS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CUIL o contraseña incorrectos"));
    }

    @Test
    void logout_validCredentials_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .with(user("20-12345678-9")))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_invalidCredentials_returns403WithError() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_invalidPassword_returns401WithError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_INVALID_PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CUIL o contraseña incorrectos"));
    }

    @Test
    void login_invalidCUIL_returns401WithError() throws Exception {
        when(userDetailsService.loadUserByUsername("20-12345678-0"))
                .thenThrow(new UsernameNotFoundException("Cuil inexistente"));
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_INVALID_CUIL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CUIL o contraseña incorrectos"));
    }

    @Test
    void login_isBlocked_returns429WithError() throws Exception {
        when(loginAttemptService.isBlocked("20-12345678-9")).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALID_CREDENTIALS))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Demasiados intentos fallidos"));
    }

    @Test
    void login_invalidPassword_registersFailure() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_INVALID_PASSWORD))
                .andExpect(status().isUnauthorized());

        verify(loginAttemptService).recordFailure("20-12345678-9");
    }

    @Test
    void login_validCredentials_clearsAttempts() throws Exception {
        when(userService.login(any())).thenReturn(new UserResponseDTO(1L, "Juan", "Perez", "20-12345678-9", Role.OPERARIO));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALID_CREDENTIALS))
                .andExpect(status().isOk());

        verify(loginAttemptService).recordSuccess("20-12345678-9");
    }
}
