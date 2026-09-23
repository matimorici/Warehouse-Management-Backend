package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.PhysicalMovementResponseDTO;
import big_three.wms.service.PhysicalMovementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PhysicalMovementController.class)
@Import(SecurityConfig.class)
class PhysicalMovementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhysicalMovementService movementService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String VALID_BODY = """
            {"idProduct": 1, "idLocationFrom": 10, "idLocationTo": 20, "idUser": 30}
            """;

    private PhysicalMovementResponseDTO response() {
        return new PhysicalMovementResponseDTO(1L, LocalDateTime.of(2026, 9, 22, 10, 0), 10L, 20L, 30L);
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(movementService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProduct").value(1))
                .andExpect(jsonPath("$.idLocationTo").value(20))
                .andExpect(jsonPath("$.idUser").value(30));
    }

    @Test
    void create_missingIdProduct_returns400() throws Exception {
        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idLocationFrom": 10, "idLocationTo": 20, "idUser": 30}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingIdLocationTo_returns400() throws Exception {
        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProduct": 1, "idLocationFrom": 10, "idUser": 30}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingIdUser_returns400() throws Exception {
        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProduct": 1, "idLocationFrom": 10, "idLocationTo": 20}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_serviceThrows_returns400() throws Exception {
        when(movementService.create(any())).thenThrow(new IllegalArgumentException("Product not found"));

        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product not found"));
    }

    @Test
    void list_returns200() throws Exception {
        when(movementService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/movimientos-fisicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProduct").value(1));
    }

    @Test
    void list_byIdProduct_returns200() throws Exception {
        when(movementService.findByIdProduct(1L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/movimientos-fisicos").param("idProduct", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLocationTo").value(20));
    }

    @Test
    void list_byIdUser_returns200() throws Exception {
        when(movementService.findByIdUser(30L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/movimientos-fisicos").param("idUser", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUser").value(30));
    }

    @Test
    void list_byDateTimeRange_returns200() throws Exception {
        when(movementService.findByDateTimeRange(any(), any())).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/movimientos-fisicos")
                        .param("from", "2026-09-01T00:00:00")
                        .param("to", "2026-09-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProduct").value(1));
    }

    @Test
    void list_onlyFrom_returns400() throws Exception {
        when(movementService.findByDateTimeRange(any(), any()))
                .thenThrow(new IllegalArgumentException("Both 'from' and 'to' are required for date range filtering"));

        mockMvc.perform(get("/api/movimientos-fisicos").param("from", "2026-09-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Both 'from' and 'to' are required for date range filtering"));
    }
}