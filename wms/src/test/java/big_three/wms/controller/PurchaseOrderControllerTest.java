package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.PurchaseOrderLineResponseDTO;
import big_three.wms.dto.PurchaseOrderResponseDTO;
import big_three.wms.service.PurchaseOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseOrderController.class)
@Import(SecurityConfig.class)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String VALID_BODY = """
            {"idSupplier": 1, "lines": [{"idProduct": 1, "amount": 2}]}
            """;

    private PurchaseOrderResponseDTO response() {
        return new PurchaseOrderResponseDTO(10L, LocalDateTime.now(), 1L, "PENDIENTE",
                List.of(new PurchaseOrderLineResponseDTO(1L, 2)));
    }

    @Test
    void create_validOrder_returns201() throws Exception {
        when(purchaseOrderService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPurchaseOrder").value(10))
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.lines[0].amount").value(2));
    }

    @Test
    void create_missingLines_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idSupplier": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_emptyLines_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idSupplier": 1, "lines": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_zeroAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idSupplier": 1, "lines": [{"idProduct": 1, "amount": 0}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(purchaseOrderService.findAllSummaries()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/ordenes-compra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPurchaseOrder").value(10));
    }

    @Test
    void search_returns200() throws Exception {
        when(purchaseOrderService.findById(10L)).thenReturn(response());

        mockMvc.perform(get("/api/ordenes-compra/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].idProduct").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(purchaseOrderService.update(eq(10L), any())).thenReturn(response());

        mockMvc.perform(put("/api/ordenes-compra/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void receive_returns200() throws Exception {
        when(purchaseOrderService.receive(10L)).thenReturn(response());

        mockMvc.perform(put("/api/ordenes-compra/10/recibir"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/ordenes-compra/10"))
                .andExpect(status().isNoContent());
    }
}