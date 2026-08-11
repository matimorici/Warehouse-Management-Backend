package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.ProveedorResponseDTO;
import big_three.wms.service.ProveedorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProveedorController.class)
@Import(SecurityConfig.class)
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProveedorService proveedorService;

    private static final String VALID_BODY = """
            {"cuit": "20-12345678-9", "razonSocial": "Razon Social", "telefono": "5555",
             "mail": "mail@test.com", "direccion": "Calle 1"}
            """;

    private ProveedorResponseDTO response() {
        return new ProveedorResponseDTO(1L, "20-12345678-9", "Razon Social", "5555", "mail@test.com", "Calle 1");
    }

    @Test
    void create_validProveedor_returns201() throws Exception {
        when(proveedorService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProveedor").value(1))
                .andExpect(jsonPath("$.razonSocial").value("Razon Social"));
    }

    @Test
    void create_invalidCuit_returns400() throws Exception {
        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cuit": "abc", "razonSocial": "Razon Social"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(proveedorService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cuit").value("20-12345678-9"));
    }

    @Test
    void search_returns200() throws Exception {
        when(proveedorService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/proveedores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProveedor").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(proveedorService.update(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/proveedores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/proveedores/1"))
                .andExpect(status().isNoContent());
    }
}
