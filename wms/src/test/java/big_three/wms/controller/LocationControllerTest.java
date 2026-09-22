package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.LocationResponseDTO;
import big_three.wms.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@Import(SecurityConfig.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String VALID_BODY = """
            {"name": "Galpón Oeste"}
            """;

    private LocationResponseDTO response() {
        return new LocationResponseDTO(1L, "Galpón Oeste");
    }

    @Test
    void create_validProveedor_returns201() throws Exception {
        when(locationService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Galpón Oeste"));
    }
    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_nameTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "AB"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns200() throws Exception {
        when(locationService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/ubicaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Galpón Oeste"));
    }

    @Test
    void search_returns200() throws Exception {
        when(locationService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/ubicaciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        when(locationService.update(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/ubicaciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/ubicaciones/1"))
                .andExpect(status().isNoContent());
    }
}
