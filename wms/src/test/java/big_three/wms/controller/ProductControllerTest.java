package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.ProductResponseDTO;
import big_three.wms.dto.StockResponseDTO;
import big_three.wms.service.ProductService;
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

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static final String VALID_BODY = """
            {"nombreProducto": "Producto", "descripcionProducto": "Desc", "codigoBarras": "779123",
             "idProveedor": 1, "origenCodigoBarras": "FABRICANTE", "cantidadDisponible": 5, "cantidadPendiente": 0}
            """;

    private ProductResponseDTO response() {
        return new ProductResponseDTO(1L, "Producto", "Desc", "779123", 1L,
                "FABRICANTE", 5, 0, LocalDateTime.now());
    }

    @Test
    void create_validProduct_returns201() throws Exception {
        when(productService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto").value(1))
                .andExpect(jsonPath("$.origenCodigoBarras").value("FABRICANTE"));
    }

    @Test
    void create_invalidOrigen_returns400() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreProducto": "Producto", "descripcionProducto": "Desc",
                                 "idProveedor": 1, "origenCodigoBarras": "XYZ"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_negativeStock_returns400() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreProducto": "Producto", "descripcionProducto": "Desc",
                                 "idProveedor": 1, "origenCodigoBarras": "FABRICANTE", "cantidadDisponible": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(productService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreProducto").value("Producto"));
    }

    @Test
    void search_returns200() throws Exception {
        when(productService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProducto").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(productService.update(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/productos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/productos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getStock_returns200() throws Exception {
        when(productService.findStockByIdProducto(1L))
                .thenReturn(new StockResponseDTO(1L, LocalDateTime.now(), 5, 0));

        mockMvc.perform(get("/api/productos/1/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(5));
    }

    @Test
    void updateStock_returns200() throws Exception {
        when(productService.updateStock(eq(1L), any()))
                .thenReturn(new StockResponseDTO(1L, LocalDateTime.now(), 9, 0));

        mockMvc.perform(put("/api/productos/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidadDisponible": 9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(9));
    }
}
