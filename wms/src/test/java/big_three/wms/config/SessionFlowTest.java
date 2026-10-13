package big_three.wms.config;

import big_three.wms.controller.AuthController;
import big_three.wms.controller.ProductController;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.model.Role;
import big_three.wms.security.LoginAttemptService;
import big_three.wms.service.BarcodeService;
import big_three.wms.service.ProductService;
import big_three.wms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, ProductController.class})
@Import(SecurityConfig.class)
class SessionFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean private UserService userService;
    @MockitoBean private LoginAttemptService loginAttemptService;
    @MockitoBean private ProductService productService;
    @MockitoBean private BarcodeService barcodeService;
    @MockitoBean private UserDetailsService userDetailsService;

    private static final String LOGIN_BODY = """
            {"cuil": "20-12345678-9", "contrasena": "Password1"}
            """;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("20-12345678-9")
                .password(passwordEncoder.encode("Password1"))
                .roles("OPERARIO")
                .build();
        when(userDetailsService.loadUserByUsername("20-12345678-9")).thenReturn(userDetails);
        when(userService.login(any())).thenReturn(new UserResponseDTO(1L, "Juan", "Perez", "20-12345678-9", Role.OPERARIO));
    }

    @Test
    void login_useSession_logout_invalidatesSession() throws Exception {
        // 1. Login: la sesión queda guardada con la identidad del usuario
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        SecurityContext saved = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(saved);
        assertEquals("20-12345678-9", saved.getAuthentication().getName());

        // 2. Con esa sesión, una ruta protegida responde
        mockMvc.perform(get("/api/productos").session(session))
                .andExpect(status().isOk());

        // 3. Sin la sesión, la misma ruta se rechaza
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isForbidden());

        // 4. Logout: la sesión queda invalidada
        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
    }
}
