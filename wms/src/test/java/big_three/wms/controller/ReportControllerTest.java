package big_three.wms.controller;

import big_three.wms.config.SecurityConfig;
import big_three.wms.dto.report.ActividadOperarioDTO;
import big_three.wms.dto.report.ProductoMasRetiradoDTO;
import big_three.wms.dto.report.ProductoSinRotacionDTO;
import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.RetiroPorPeriodoDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.model.Role;
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

import java.time.LocalDate;
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

    @Test
    void productosMasRetirados_returns200WithFields() throws Exception {
        when(reportService.findProductosMasRetirados(null, null, null)).thenReturn(List.of(
                new ProductoMasRetiradoDTO(1L, "Producto", "779123", false, 8L, 2L)));

        mockMvc.perform(get("/api/reportes/productos/mas-retirados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProducto").value(1))
                .andExpect(jsonPath("$[0].nombreProducto").value("Producto"))
                .andExpect(jsonPath("$[0].codigoBarras").value("779123"))
                .andExpect(jsonPath("$[0].activo").value(false))
                .andExpect(jsonPath("$[0].unidadesRetiradas").value(8))
                .andExpect(jsonPath("$[0].cantidadOrdenes").value(2));
    }

    @Test
    void productosMasRetirados_withDatesAndLimit_passesThemToService() throws Exception {
        when(reportService.findProductosMasRetirados(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/productos/mas-retirados")
                        .param("from", "2026-03-10")
                        .param("to", "2026-03-12")
                        .param("limit", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void productosMasRetirados_invalidLimit_returns400() throws Exception {
        when(reportService.findProductosMasRetirados(null, null, 0))
                .thenThrow(new IllegalArgumentException("El límite debe estar entre 1 y 100"));

        mockMvc.perform(get("/api/reportes/productos/mas-retirados").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El límite debe estar entre 1 y 100"));
    }

    @Test
    void productosMasRetirados_malformedFrom_returns400WithParameterName() throws Exception {
        mockMvc.perform(get("/api/reportes/productos/mas-retirados").param("from", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'from' tiene un valor inválido"));
    }

    @Test
    void productosSinRotacion_returns200WithFields() throws Exception {
        when(reportService.findProductosSinRotacion(null, null, null)).thenReturn(List.of(
                new ProductoSinRotacionDTO(2L, "Sin movimiento", "779999", 4,
                        LocalDateTime.of(2025, 12, 1, 9, 30))));

        mockMvc.perform(get("/api/reportes/productos/sin-rotacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProducto").value(2))
                .andExpect(jsonPath("$[0].nombreProducto").value("Sin movimiento"))
                .andExpect(jsonPath("$[0].codigoBarras").value("779999"))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(4))
                .andExpect(jsonPath("$[0].ultimoRetiro").exists());
    }

    @Test
    void productosSinRotacion_withDatesAndSoloConStock_passesThemToService() throws Exception {
        when(reportService.findProductosSinRotacion(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), false)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/productos/sin-rotacion")
                        .param("from", "2026-03-10")
                        .param("to", "2026-03-12")
                        .param("soloConStock", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void retirosPorPeriodo_returns200WithFields() throws Exception {
        when(reportService.findRetirosPorPeriodo(null, null, null)).thenReturn(List.of(
                new RetiroPorPeriodoDTO(LocalDate.of(2026, 3, 9), 2L, 7L)));

        mockMvc.perform(get("/api/reportes/retiros/por-periodo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodo").value("2026-03-09"))
                .andExpect(jsonPath("$[0].cantidadOrdenes").value(2))
                .andExpect(jsonPath("$[0].unidadesRetiradas").value(7));
    }

    @Test
    void retirosPorPeriodo_invalidAgrupacion_returns400() throws Exception {
        when(reportService.findRetirosPorPeriodo(null, null, "ANIO"))
                .thenThrow(new IllegalArgumentException("La agrupación debe ser DIA, SEMANA o MES"));

        mockMvc.perform(get("/api/reportes/retiros/por-periodo").param("agrupacion", "ANIO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La agrupación debe ser DIA, SEMANA o MES"));
    }

    @Test
    void operariosActividad_returns200WithFields() throws Exception {
        when(reportService.findActividadOperarios(null, null)).thenReturn(List.of(
                new ActividadOperarioDTO(1L, "Ana", "Alvarez", "20-11111111-1", Role.OPERARIO,
                        false, 2L, 5L, 3L, LocalDateTime.of(2026, 3, 10, 10, 0))));

        mockMvc.perform(get("/api/reportes/operarios/actividad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[0].apellido").value("Alvarez"))
                .andExpect(jsonPath("$[0].cuil").value("20-11111111-1"))
                .andExpect(jsonPath("$[0].rol").value("OPERARIO"))
                .andExpect(jsonPath("$[0].activo").value(false))
                .andExpect(jsonPath("$[0].cantidadRetiros").value(2))
                .andExpect(jsonPath("$[0].unidadesRetiradas").value(5))
                .andExpect(jsonPath("$[0].cantidadMovimientos").value(3))
                .andExpect(jsonPath("$[0].ultimaActividad").exists())
                .andExpect(jsonPath("$[0].contrasena").doesNotExist());
    }

    @Test
    void operariosActividad_malformedTo_returns400WithParameterName() throws Exception {
        mockMvc.perform(get("/api/reportes/operarios/actividad").param("to", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'to' tiene un valor inválido"));
    }
}
