package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.MovimientoFisicoResponseDTO;
import big_three.wms.service.MovimientoFisicoService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovimientoFisicoController.class)
@Import(SecurityConfig.class)
class MovimientoFisicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovimientoFisicoService movimientoFisicoService;

    private static final String VALID_BODY = """
            {"idProducto": 1, "idUbicacionDesde": 2, "idUbicacionHasta": 3, "idUsuario": 4}
            """;

    private MovimientoFisicoResponseDTO response() {
        return new MovimientoFisicoResponseDTO(1L, LocalDateTime.now(), 2L, 3L, 4L);
    }

    @Test
    void create_validMovimiento_returns201() throws Exception {
        when(movimientoFisicoService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto").value(1))
                .andExpect(jsonPath("$.idUbicacionHasta").value(3));
    }

    @Test
    void create_missingUbicacionHasta_returns400() throws Exception {
        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProducto": 1, "idUsuario": 4}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingProducto_returns400() throws Exception {
        mockMvc.perform(post("/api/movimientos-fisicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idUbicacionHasta": 3, "idUsuario": 4}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(movimientoFisicoService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/movimientos-fisicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(4));
    }

    @Test
    void listByProducto_returns200() throws Exception {
        when(movimientoFisicoService.findByProducto(1L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/movimientos-fisicos/producto/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProducto").value(1));
    }

    @Test
    void search_returns200() throws Exception {
        LocalDateTime fecha = LocalDateTime.now();
        when(movimientoFisicoService.findByIdProductoAndFechaHora(1L, fecha)).thenReturn(response());

        mockMvc.perform(get("/api/movimientos-fisicos/producto/1/fecha/" + fecha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProducto").value(1));
    }
}
