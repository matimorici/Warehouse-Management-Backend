package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.LineaCompraResponseDTO;
import big_three.wms.dto.OrdenCompraResponseDTO;
import big_three.wms.service.OrdenCompraService;
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

@WebMvcTest(OrdenCompraController.class)
@Import(SecurityConfig.class)
class OrdenCompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdenCompraService ordenCompraService;

    private static final String VALID_BODY = """
            {"idProveedor": 1, "lineasCompra": [{"idProducto": 1, "cantidad": 2}]}
            """;

    private OrdenCompraResponseDTO response() {
        return new OrdenCompraResponseDTO(10L, LocalDateTime.now(), 1L, "PENDIENTE",
                List.of(new LineaCompraResponseDTO(1L, 2)));
    }

    @Test
    void create_validOrder_returns201() throws Exception {
        when(ordenCompraService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idOrdenCompra").value(10))
                .andExpect(jsonPath("$.lineasCompra[0].cantidad").value(2));
    }

    @Test
    void create_zeroCantidad_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProveedor": 1, "lineasCompra": [{"idProducto": 1, "cantidad": 0}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingLineas_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProveedor": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_emptyLineas_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProveedor": 1, "lineasCompra": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(ordenCompraService.findAllSummaries()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/ordenes-compra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idOrdenCompra").value(10));
    }

    @Test
    void search_returns200() throws Exception {
        when(ordenCompraService.findById(10L)).thenReturn(response());

        mockMvc.perform(get("/api/ordenes-compra/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineasCompra[0].idProducto").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(ordenCompraService.update(eq(10L), any(), eq("RECIBIDA"))).thenReturn(response());

        mockMvc.perform(put("/api/ordenes-compra/10?estado=RECIBIDA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/ordenes-compra/10"))
                .andExpect(status().isNoContent());
    }
}
