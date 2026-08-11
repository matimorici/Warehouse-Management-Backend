package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.PickOrderLineResponseDTO;
import big_three.wms.dto.PickOrderResponseDTO;
import big_three.wms.service.PickOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PickOrderController.class)
@Import(SecurityConfig.class)
class PickOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PickOrderService pickOrderService;

    private static final String VALID_BODY = """
            {"idUsuario": 1, "lineasRetiro": [{"idProducto": 1, "cantidad": 2}]}
            """;

    private PickOrderResponseDTO response() {
        return new PickOrderResponseDTO(10L, LocalDateTime.now(), 1L,
                List.of(new PickOrderLineResponseDTO(1L, 2)));
    }

    @Test
    void create_validOrder_returns201() throws Exception {
        when(pickOrderService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/ordenes-retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idOrdenRetiro").value(10))
                .andExpect(jsonPath("$.lineasRetiro[0].cantidad").value(2));
    }

    @Test
    void create_zeroCantidad_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idUsuario": 1, "lineasRetiro": [{"idProducto": 1, "cantidad": 0}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingLineas_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idUsuario": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(pickOrderService.findAllSummaries()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/ordenes-retiro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idOrdenRetiro").value(10));
    }

    @Test
    void search_returns200() throws Exception {
        when(pickOrderService.findById(10L)).thenReturn(response());

        mockMvc.perform(get("/api/ordenes-retiro/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineasRetiro[0].idProducto").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(pickOrderService.update(eq(10L), any())).thenReturn(response());

        mockMvc.perform(put("/api/ordenes-retiro/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/ordenes-retiro/10"))
                .andExpect(status().isNoContent());
    }
}
