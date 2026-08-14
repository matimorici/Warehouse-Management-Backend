package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.ValoracionProveedorResponseDTO;
import big_three.wms.service.ValoracionProveedorService;
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

@WebMvcTest(ValoracionProveedorController.class)
@Import(SecurityConfig.class)
class ValoracionProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValoracionProveedorService valoracionProveedorService;

    private static final String VALID_BODY = """
            {"idProveedor": 1, "tiempoEntrega": 3, "formaEntrega": "Presencial",
             "relacionPrecioCalidad": "Buena"}
            """;

    private ValoracionProveedorResponseDTO response() {
        return new ValoracionProveedorResponseDTO(1L, 1L, LocalDateTime.now(), 3, "Presencial", "Buena");
    }

    @Test
    void create_validValoracion_returns201() throws Exception {
        when(valoracionProveedorService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idValoracion").value(1))
                .andExpect(jsonPath("$.idProveedor").value(1));
    }

    @Test
    void create_missingProveedor_returns400() throws Exception {
        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiempoEntrega": 3}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_negativeTiempoEntrega_returns400() throws Exception {
        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProveedor": 1, "tiempoEntrega": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_tooLongFormaEntrega_returns400() throws Exception {
        String formaEntrega = "a".repeat(101);
        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProveedor": 1, "formaEntrega": "%s"}
                                """.formatted(formaEntrega)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_tooLongRelacionPrecioCalidad_returns400() throws Exception {
        String relacion = "a".repeat(101);
        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProveedor": 1, "relacionPrecioCalidad": "%s"}
                                """.formatted(relacion)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_onlyProveedor_returns201() throws Exception {
        when(valoracionProveedorService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/valoraciones-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProveedor": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProveedor").value(1));
    }

    @Test
    void list_returns200() throws Exception {
        when(valoracionProveedorService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/valoraciones-proveedor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProveedor").value(1));
    }

    @Test
    void search_returns200() throws Exception {
        when(valoracionProveedorService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/valoraciones-proveedor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idValoracion").value(1));
    }

    @Test
    void listByProveedor_returns200() throws Exception {
        when(valoracionProveedorService.findByProveedor(1L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/valoraciones-proveedor/proveedor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProveedor").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(valoracionProveedorService.update(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/valoraciones-proveedor/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_invalidBody_returns400() throws Exception {
        mockMvc.perform(put("/api/valoraciones-proveedor/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiempoEntrega": 3}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/valoraciones-proveedor/1"))
                .andExpect(status().isNoContent());
    }
}
