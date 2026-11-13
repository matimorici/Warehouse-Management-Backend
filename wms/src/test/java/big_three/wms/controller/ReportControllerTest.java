package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMINISTRADOR")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void stockBajo_returns200WithFields() throws Exception {
        when(reportService.findStockBajo(null)).thenReturn(List.of(new StockBajoDTO(
                1L, "Producto", "779123", 2L, "Proveedor", 3, 1, LocalDateTime.now())));

        mockMvc.perform(get("/api/reportes/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProducto").value(1))
                .andExpect(jsonPath("$[0].nombreProducto").value("Producto"))
                .andExpect(jsonPath("$[0].codigoBarras").value("779123"))
                .andExpect(jsonPath("$[0].idProveedor").value(2))
                .andExpect(jsonPath("$[0].razonSocial").value("Proveedor"))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(3))
                .andExpect(jsonPath("$[0].cantidadPendiente").value(1));
    }

    @Test
    void stockBajo_withUmbral_passesItToService() throws Exception {
        when(reportService.findStockBajo(3)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/stock-bajo").param("umbral", "3"))
                .andExpect(status().isOk());
    }

    @Test
    void stockBajo_negativeUmbral_returns400() throws Exception {
        when(reportService.findStockBajo(-1))
                .thenThrow(new IllegalArgumentException("El umbral no puede ser negativo"));

        mockMvc.perform(get("/api/reportes/stock-bajo").param("umbral", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El umbral no puede ser negativo"));
    }

    @Test
    void stockBajo_nonNumericUmbral_returns400WithParameterName() throws Exception {
        mockMvc.perform(get("/api/reportes/stock-bajo").param("umbral", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'umbral' tiene un valor inválido"));
    }

    @Test
    void stockPorProveedor_returns200WithFields() throws Exception {
        when(reportService.findStockPorProveedor()).thenReturn(List.of(
                new StockPorProveedorDTO(1L, "Proveedor", 2L, 15L, 3L)));

        mockMvc.perform(get("/api/reportes/stock-por-proveedor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProveedor").value(1))
                .andExpect(jsonPath("$[0].razonSocial").value("Proveedor"))
                .andExpect(jsonPath("$[0].cantidadProductos").value(2))
                .andExpect(jsonPath("$[0].unidadesDisponibles").value(15))
                .andExpect(jsonPath("$[0].unidadesPendientes").value(3));
    }

    @Test
    void productosUbicacionActual_returns200WithFields() throws Exception {
        when(reportService.findProductosUbicacionActual(null)).thenReturn(List.of(
                new ProductoUbicacionActualDTO(1L, "Producto", "779123", 2L, "Estante A",
                        LocalDateTime.now())));

        mockMvc.perform(get("/api/reportes/productos/ubicacion-actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProducto").value(1))
                .andExpect(jsonPath("$[0].nombreProducto").value("Producto"))
                .andExpect(jsonPath("$[0].codigoBarras").value("779123"))
                .andExpect(jsonPath("$[0].idUbicacion").value(2))
                .andExpect(jsonPath("$[0].nombreUbicacion").value("Estante A"))
                .andExpect(jsonPath("$[0].fechaUltimoMovimiento").exists());
    }

    @Test
    void productosUbicacionActual_soloSinUbicar_passesTrue() throws Exception {
        when(reportService.findProductosUbicacionActual(true)).thenReturn(List.of(
                new ProductoUbicacionActualDTO(1L, "Producto", "779123", null, null, null)));

        mockMvc.perform(get("/api/reportes/productos/ubicacion-actual").param("soloSinUbicar", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUbicacion").doesNotExist())
                .andExpect(jsonPath("$[0].nombreUbicacion").doesNotExist())
                .andExpect(jsonPath("$[0].fechaUltimoMovimiento").doesNotExist());
    }

    @Test
    void ubicacionesOcupacion_returns200WithFields() throws Exception {
        when(reportService.findUbicacionesOcupacion()).thenReturn(List.of(
                new UbicacionOcupacionDTO(1L, "Galpón Oeste", 4L)));

        mockMvc.perform(get("/api/reportes/ubicaciones/ocupacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUbicacion").value(1))
                .andExpect(jsonPath("$[0].nombreUbicacion").value("Galpón Oeste"))
                .andExpect(jsonPath("$[0].cantidadProductos").value(4));
    }
}
