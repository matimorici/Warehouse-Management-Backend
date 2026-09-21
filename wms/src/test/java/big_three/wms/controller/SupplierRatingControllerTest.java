package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.SupplierRatingResponseDTO;
import big_three.wms.service.SupplierRatingService;
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

@WebMvcTest(SupplierRatingController.class)
@Import(SecurityConfig.class)
class SupplierRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierRatingService ratingService;

    private static final String VALID_BODY = """
            {"idSupplier": 1, "deliveryTime": 2, "deliveryMethod": "Entrega a domicilio",
             "priceQualityRatio": "Buena"}
            """;

    private SupplierRatingResponseDTO response() {
        return new SupplierRatingResponseDTO(
                1L, LocalDateTime.of(2026, 9, 21, 10, 0), 1L, 2,
                "Entrega a domicilio", "Buena");
    }

    @Test
    void create_validRating_returns201() throws Exception {
        when(ratingService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.idSupplier").value(1));
    }

    @Test
    void create_missingIdSupplier_returns400() throws Exception {
        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryTime": 2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_negativeDeliveryTime_returns400() throws Exception {
        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idSupplier": 1, "deliveryTime": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(ratingService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/valoraciones-proveedor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSupplier").value(1));
    }

    @Test
    void list_bySupplier_returns200() throws Exception {
        when(ratingService.findBySupplier(1L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/valoraciones-proveedor").param("idSupplier", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryMethod").value("Entrega a domicilio"));
    }

    @Test
    void search_returns200() throws Exception {
        when(ratingService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/valoraciones-proveedor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(ratingService.update(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/valoraciones-proveedor/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/valoraciones-proveedor/1"))
                .andExpect(status().isNoContent());
    }
}