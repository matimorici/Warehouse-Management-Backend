package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.UbicacionResponseDTO;
import big_three.wms.service.UbicacionService;
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

@WebMvcTest(UbicacionController.class)
@Import(SecurityConfig.class)
class UbicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UbicacionService ubicacionService;

    private static final String VALID_BODY = """
            {"nombreUbicacion": "Estanteria A1"}
            """;

    private UbicacionResponseDTO response() {
        return new UbicacionResponseDTO(1L, "Estanteria A1");
    }

    @Test
    void create_validUbicacion_returns201() throws Exception {
        when(ubicacionService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUbicacion").value(1))
                .andExpect(jsonPath("$.nombreUbicacion").value("Estanteria A1"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUbicacion": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_tooShortName_returns400() throws Exception {
        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUbicacion": "AB"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_tooLongName_returns400() throws Exception {
        String nombre = "a".repeat(101);
        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUbicacion": "%s"}
                                """.formatted(nombre)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(ubicacionService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/ubicaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreUbicacion").value("Estanteria A1"));
    }

    @Test
    void search_returns200() throws Exception {
        when(ubicacionService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/ubicaciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUbicacion").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(ubicacionService.update(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/ubicaciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_blankName_returns400() throws Exception {
        mockMvc.perform(put("/api/ubicaciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUbicacion": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/ubicaciones/1"))
                .andExpect(status().isNoContent());
    }
}
