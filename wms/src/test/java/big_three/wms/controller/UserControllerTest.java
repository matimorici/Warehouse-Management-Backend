package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final String VALID_BODY = """
            {"nombre": "Juan", "apellido": "Perez", "cuil": "20-12345678-9", "contrasena": "Password1"}
            """;

    @Test
    void create_validUser_returns201() throws Exception {
        when(userService.create(any())).thenReturn(new UserResponseDTO(1L, "Juan", "Perez", "20-12345678-9", "OPERARIO"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.rol").value("OPERARIO"));
    }

    @Test
    void create_invalidCuil_returns400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Juan", "apellido": "Perez", "cuil": "abc", "contrasena": "Password1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Juan", "apellido": "Perez", "cuil": "20-12345678-9", "contrasena": "short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(userService.findAll()).thenReturn(List.of(new UserResponseDTO(1L, "Juan", "Perez", "20-12345678-9", "OPERARIO")));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Juan"));
    }
}
