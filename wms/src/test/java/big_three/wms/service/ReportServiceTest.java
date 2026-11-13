package big_three.wms.service;

import big_three.wms.dto.report.ActividadOperarioDTO;
import big_three.wms.dto.report.OrdenCompraResumenDTO;
import big_three.wms.dto.report.ProductoMasRetiradoDTO;
import big_three.wms.dto.report.ProductoSinRotacionDTO;
import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.ProveedorRankingDTO;
import big_three.wms.dto.report.RetiroPorPeriodoDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.model.Role;
import big_three.wms.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void findStockBajo_sinUmbral_usaDiezPorDefecto() {
        List<StockBajoDTO> esperado = List.of(new StockBajoDTO(
                1L, "Producto", "779123", 2L, "Proveedor", 3, 1, LocalDateTime.now()));
        when(reportRepository.findStockBajo(10)).thenReturn(esperado);

        List<StockBajoDTO> resultado = reportService.findStockBajo(null);

        assertEquals(esperado, resultado);
        verify(reportRepository).findStockBajo(10);
    }

    @Test
    void findStockBajo_conUmbral_usaElValorRecibido() {
        List<StockBajoDTO> esperado = List.of(new StockBajoDTO(
                1L, "Producto", "779123", 2L, "Proveedor", 0, 0, LocalDateTime.now()));
        when(reportRepository.findStockBajo(0)).thenReturn(esperado);

        List<StockBajoDTO> resultado = reportService.findStockBajo(0);

        assertEquals(esperado, resultado);
        verify(reportRepository).findStockBajo(0);
    }

    @Test
    void findStockBajo_umbralNegativo_lanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reportService.findStockBajo(-1));

        assertEquals("El umbral no puede ser negativo", ex.getMessage());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void findStockPorProveedor_devuelveLasFilasDelRepositorio() {
        List<StockPorProveedorDTO> esperado = List.of(
                new StockPorProveedorDTO(1L, "Proveedor", 2L, 15L, 3L));
        when(reportRepository.findStockPorProveedor()).thenReturn(esperado);

        assertEquals(esperado, reportService.findStockPorProveedor());
    }

    @Test
    void findProductosUbicacionActual_mapeaLaProyeccionAlDto() {
        ReportRepository.ProductoUbicacionActualProjection fila =
                mock(ReportRepository.ProductoUbicacionActualProjection.class);
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 2, 3, 4);
        when(fila.getIdProducto()).thenReturn(1L);
        when(fila.getNombreProducto()).thenReturn("Producto");
        when(fila.getCodigoBarras()).thenReturn("779123");
        when(fila.getIdUbicacion()).thenReturn(2L);
        when(fila.getNombreUbicacion()).thenReturn("Estante A");
        when(fila.getFechaUltimoMovimiento()).thenReturn(fecha);
        when(reportRepository.findProductosUbicacionActual(false)).thenReturn(List.of(fila));

        List<ProductoUbicacionActualDTO> resultado =
                reportService.findProductosUbicacionActual(null);

        assertEquals(1, resultado.size());
        ProductoUbicacionActualDTO dto = resultado.get(0);
        assertEquals(1L, dto.getIdProducto());
        assertEquals("Producto", dto.getNombreProducto());
        assertEquals("779123", dto.getCodigoBarras());
        assertEquals(2L, dto.getIdUbicacion());
        assertEquals("Estante A", dto.getNombreUbicacion());
        assertEquals(fecha, dto.getFechaUltimoMovimiento());
        verify(reportRepository).findProductosUbicacionActual(false);
    }

    @Test
    void findProductosUbicacionActual_soloSinUbicarTrue_pasaElFiltro() {
        when(reportRepository.findProductosUbicacionActual(true)).thenReturn(List.of());

        assertTrue(reportService.findProductosUbicacionActual(true).isEmpty());
        verify(reportRepository).findProductosUbicacionActual(true);
    }

    @Test
    void findProductosUbicacionActual_soloSinUbicarNull_usaFalse() {
        when(reportRepository.findProductosUbicacionActual(false)).thenReturn(List.of());

        assertTrue(reportService.findProductosUbicacionActual(null).isEmpty());
        verify(reportRepository).findProductosUbicacionActual(false);
    }

    @Test
    void findUbicacionesOcupacion_mapeaLaProyeccionAlDto() {
        ReportRepository.UbicacionOcupacionProjection fila =
                mock(ReportRepository.UbicacionOcupacionProjection.class);
        when(fila.getIdUbicacion()).thenReturn(3L);
        when(fila.getNombreUbicacion()).thenReturn("Galpón Oeste");
        when(fila.getCantidadProductos()).thenReturn(4L);
        when(reportRepository.findUbicacionesOcupacion()).thenReturn(List.of(fila));

        List<UbicacionOcupacionDTO> resultado = reportService.findUbicacionesOcupacion();

        assertEquals(1, resultado.size());
        assertEquals(3L, resultado.get(0).getIdUbicacion());
        assertEquals("Galpón Oeste", resultado.get(0).getNombreUbicacion());
        assertEquals(4L, resultado.get(0).getCantidadProductos());
    }

    @Test
    void findProductosMasRetirados_sinLimit_usaDiezYResuelveElPeriodo() {
        ReportRepository.ProductoMasRetiradoProjection fila =
                mock(ReportRepository.ProductoMasRetiradoProjection.class);
        when(fila.getIdProducto()).thenReturn(1L);
        when(fila.getNombreProducto()).thenReturn("Producto");
        when(fila.getCodigoBarras()).thenReturn("779123");
        when(fila.getActivo()).thenReturn(false);
        when(fila.getUnidadesRetiradas()).thenReturn(8L);
        when(fila.getCantidadOrdenes()).thenReturn(2L);
        when(reportRepository.findProductosMasRetirados(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0),
                10)).thenReturn(List.of(fila));

        List<ProductoMasRetiradoDTO> resultado = reportService.findProductosMasRetirados(null, null, null);

        assertEquals(1, resultado.size());
        ProductoMasRetiradoDTO dto = resultado.get(0);
        assertEquals(1L, dto.getIdProducto());
        assertEquals("Producto", dto.getNombreProducto());
        assertEquals("779123", dto.getCodigoBarras());
        assertFalse(dto.getActivo());
        assertEquals(8L, dto.getUnidadesRetiradas());
        assertEquals(2L, dto.getCantidadOrdenes());
    }

    @Test
    void findProductosMasRetirados_conLimitYFechas_pasaElRangoResuelto() {
        when(reportRepository.findProductosMasRetirados(
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 13, 0, 0),
                5)).thenReturn(List.of());

        assertTrue(reportService.findProductosMasRetirados(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), 5).isEmpty());
    }

    @Test
    void findProductosMasRetirados_limitCero_lanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reportService.findProductosMasRetirados(null, null, 0));

        assertEquals("El límite debe estar entre 1 y 100", ex.getMessage());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void findProductosMasRetirados_limitMayorACien_lanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reportService.findProductosMasRetirados(null, null, 101));

        assertEquals("El límite debe estar entre 1 y 100", ex.getMessage());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void findProductosSinRotacion_sinSoloConStock_usaTrue() {
        when(reportRepository.findProductosSinRotacion(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0),
                true)).thenReturn(List.of());

        assertTrue(reportService.findProductosSinRotacion(null, null, null).isEmpty());
    }

    @Test
    void findProductosSinRotacion_conSoloConStockFalse_pasaFalse() {
        when(reportRepository.findProductosSinRotacion(
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 11, 0, 0),
                false)).thenReturn(List.of());

        assertTrue(reportService.findProductosSinRotacion(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10), false).isEmpty());
    }

    @Test
    void findProductosSinRotacion_mapeaLaProyeccionAlDto() {
        LocalDateTime ultimoRetiro = LocalDateTime.of(2025, 12, 1, 9, 30);
        ProductoSinRotacionDTO esperado = new ProductoSinRotacionDTO(
                2L, "Sin movimiento", "779999", 4, ultimoRetiro);
        when(reportRepository.findProductosSinRotacion(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0),
                true)).thenReturn(List.of(esperado));

        assertEquals(List.of(esperado), reportService.findProductosSinRotacion(null, null, true));
    }

    @Test
    void findRetirosPorPeriodo_sinAgrupacion_usaDia() {
        ReportRepository.RetiroPorPeriodoProjection fila =
                mock(ReportRepository.RetiroPorPeriodoProjection.class);
        LocalDate periodo = LocalDate.of(2026, 3, 10);
        when(fila.getPeriodo()).thenReturn(periodo);
        when(fila.getCantidadOrdenes()).thenReturn(2L);
        when(fila.getUnidadesRetiradas()).thenReturn(7L);
        when(reportRepository.findRetirosPorPeriodo(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0),
                "day")).thenReturn(List.of(fila));

        List<RetiroPorPeriodoDTO> resultado = reportService.findRetirosPorPeriodo(null, null, null);

        assertEquals(1, resultado.size());
        assertEquals(periodo, resultado.get(0).getPeriodo());
        assertEquals(2L, resultado.get(0).getCantidadOrdenes());
        assertEquals(7L, resultado.get(0).getUnidadesRetiradas());
    }

    @Test
    void findRetirosPorPeriodo_semanaInsensibleAMayusculas_usaWeek() {
        when(reportRepository.findRetirosPorPeriodo(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0),
                "week")).thenReturn(List.of());

        assertTrue(reportService.findRetirosPorPeriodo(null, null, "semana").isEmpty());
    }

    @Test
    void findRetirosPorPeriodo_mes_usaMonth() {
        when(reportRepository.findRetirosPorPeriodo(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0),
                "month")).thenReturn(List.of());

        assertTrue(reportService.findRetirosPorPeriodo(null, null, "MES").isEmpty());
    }

    @Test
    void findRetirosPorPeriodo_agrupacionInvalida_lanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reportService.findRetirosPorPeriodo(null, null, "ANIO"));

        assertEquals("La agrupación debe ser DIA, SEMANA o MES", ex.getMessage());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void findActividadOperarios_combinaRetirosYMovimientosPorUsuario() {
        LocalDateTime ultimaRetiro1 = LocalDateTime.of(2026, 3, 10, 10, 0);
        LocalDateTime ultimaMovimiento2 = LocalDateTime.of(2026, 3, 11, 9, 0);
        LocalDateTime ultimaRetiro4 = LocalDateTime.of(2026, 3, 10, 8, 0);
        LocalDateTime ultimaMovimiento4 = LocalDateTime.of(2026, 3, 12, 9, 0);

        ReportRepository.UsuarioActividadBaseProjection usuario1 =
                usuario(1L, "Ana", "Alvarez", "20-11111111-1", "OPERARIO", true);
        ReportRepository.UsuarioActividadBaseProjection usuario2 =
                usuario(2L, "Beto", "Benitez", "20-22222222-2", "ADMINISTRADOR", true);
        ReportRepository.UsuarioActividadBaseProjection usuario3 =
                usuario(3L, "Carla", "Costa", "20-33333333-3", "OPERARIO", false);
        ReportRepository.UsuarioActividadBaseProjection usuario4 =
                usuario(4L, "Diego", "Diaz", "20-44444444-4", "OPERARIO", true);

        ReportRepository.RetirosUsuarioProjection retiros1 =
                retiros(1L, 2L, 5L, ultimaRetiro1);
        ReportRepository.RetirosUsuarioProjection retiros4 =
                retiros(4L, 1L, 2L, ultimaRetiro4);
        ReportRepository.MovimientosUsuarioProjection movimientos2 =
                movimientos(2L, 3L, ultimaMovimiento2);
        ReportRepository.MovimientosUsuarioProjection movimientos4 =
                movimientos(4L, 1L, ultimaMovimiento4);

        when(reportRepository.findUsuariosActividad())
                .thenReturn(List.of(usuario1, usuario2, usuario3, usuario4));
        when(reportRepository.findRetirosPorUsuario(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0)))
                .thenReturn(List.of(retiros1, retiros4));
        when(reportRepository.findMovimientosPorUsuario(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0)))
                .thenReturn(List.of(movimientos2, movimientos4));

        List<ActividadOperarioDTO> resultado = reportService.findActividadOperarios(null, null);

        assertEquals(4, resultado.size());
        assertEquals(List.of(2L, 1L, 4L, 3L),
                resultado.stream().map(ActividadOperarioDTO::getIdUsuario).toList());

        ActividadOperarioDTO soloRetiros = resultado.get(1);
        assertEquals(2L, soloRetiros.getCantidadRetiros());
        assertEquals(5L, soloRetiros.getUnidadesRetiradas());
        assertEquals(0L, soloRetiros.getCantidadMovimientos());
        assertEquals(ultimaRetiro1, soloRetiros.getUltimaActividad());
        assertTrue(soloRetiros.getActivo());
        assertEquals(Role.OPERARIO, soloRetiros.getRol());

        ActividadOperarioDTO soloMovimientos = resultado.get(0);
        assertEquals(0L, soloMovimientos.getCantidadRetiros());
        assertEquals(0L, soloMovimientos.getUnidadesRetiradas());
        assertEquals(3L, soloMovimientos.getCantidadMovimientos());
        assertEquals(ultimaMovimiento2, soloMovimientos.getUltimaActividad());

        ActividadOperarioDTO ambos = resultado.get(2);
        assertEquals(1L, ambos.getCantidadRetiros());
        assertEquals(2L, ambos.getUnidadesRetiradas());
        assertEquals(1L, ambos.getCantidadMovimientos());
        assertEquals(ultimaMovimiento4, ambos.getUltimaActividad());

        ActividadOperarioDTO sinActividad = resultado.get(3);
        assertEquals(0L, sinActividad.getCantidadRetiros());
        assertEquals(0L, sinActividad.getUnidadesRetiradas());
        assertEquals(0L, sinActividad.getCantidadMovimientos());
        assertNull(sinActividad.getUltimaActividad());
        assertFalse(sinActividad.getActivo());
    }

    @Test
    void findOrdenesCompraResumen_mapeaLaProyeccionYResuelveElPeriodo() {
        ReportRepository.OrdenCompraResumenProjection fila =
                mock(ReportRepository.OrdenCompraResumenProjection.class);
        when(fila.getIdProveedor()).thenReturn(4L);
        when(fila.getRazonSocial()).thenReturn("Proveedor Dado de Baja");
        when(fila.getActivo()).thenReturn(false);
        when(fila.getOrdenesPendientes()).thenReturn(2L);
        when(fila.getOrdenesRecibidas()).thenReturn(3L);
        when(fila.getOrdenesCanceladas()).thenReturn(1L);
        when(fila.getUnidadesPedidas()).thenReturn(25L);
        when(fila.getUnidadesRecibidas()).thenReturn(10L);
        when(reportRepository.findOrdenesCompraResumen(
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 13, 0, 0)))
                .thenReturn(List.of(fila));

        List<OrdenCompraResumenDTO> resultado = reportService.findOrdenesCompraResumen(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));

        assertEquals(1, resultado.size());
        OrdenCompraResumenDTO dto = resultado.get(0);
        assertEquals(4L, dto.getIdProveedor());
        assertEquals("Proveedor Dado de Baja", dto.getRazonSocial());
        assertFalse(dto.getActivo());
        assertEquals(2L, dto.getOrdenesPendientes());
        assertEquals(3L, dto.getOrdenesRecibidas());
        assertEquals(1L, dto.getOrdenesCanceladas());
        assertEquals(25L, dto.getUnidadesPedidas());
        assertEquals(10L, dto.getUnidadesRecibidas());
    }

    @Test
    void findOrdenesCompraResumen_sinFechas_usaElPeriodoPorDefecto() {
        when(reportRepository.findOrdenesCompraResumen(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0)))
                .thenReturn(List.of());

        assertTrue(reportService.findOrdenesCompraResumen(null, null).isEmpty());
    }

    @Test
    void findProveedoresRanking_combinaValoracionesYOrdenesPorProveedor() {
        ReportRepository.ProveedorRankingBaseProjection soloValoraciones =
                proveedorRanking(1L, "Proveedor A", true);
        ReportRepository.ProveedorRankingBaseProjection soloOrdenes =
                proveedorRanking(2L, "Proveedor B", true);
        ReportRepository.ProveedorRankingBaseProjection sinActividad =
                proveedorRanking(3L, "Proveedor C", false);
        ReportRepository.ProveedorRankingBaseProjection ambos =
                proveedorRanking(4L, "Proveedor D", true);

        ReportRepository.ValoracionesProveedorProjection valoraciones1 =
                valoraciones(1L, 2L, 4.5);
        ReportRepository.ValoracionesProveedorProjection valoraciones4 =
                valoraciones(4L, 1L, 3.0);
        ReportRepository.OrdenesRecibidasProveedorProjection ordenes2 =
                ordenes(2L, 2L, 3.333333, 1L);
        ReportRepository.OrdenesRecibidasProveedorProjection ordenes4 =
                ordenes(4L, 1L, 1.236, 2L);

        when(reportRepository.findProveedoresRankingBase(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0)))
                .thenReturn(List.of(soloValoraciones, soloOrdenes, sinActividad, ambos));
        when(reportRepository.findValoracionesPorProveedor(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0)))
                .thenReturn(List.of(valoraciones1, valoraciones4));
        when(reportRepository.findOrdenesRecibidasPorProveedor(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(9999, 1, 1, 0, 0)))
                .thenReturn(List.of(ordenes2, ordenes4));

        List<ProveedorRankingDTO> resultado = reportService.findProveedoresRanking(null, null);

        assertEquals(List.of(4L, 2L, 1L, 3L),
                resultado.stream().map(ProveedorRankingDTO::getIdProveedor).toList());

        ProveedorRankingDTO ambosDto = resultado.get(0);
        assertEquals("Proveedor D", ambosDto.getRazonSocial());
        assertTrue(ambosDto.getActivo());
        assertEquals(1L, ambosDto.getCantidadValoraciones());
        assertEquals(3.0, ambosDto.getPromedioTiempoEntregaValorado());
        assertEquals(1L, ambosDto.getOrdenesRecibidas());
        assertEquals(1.24, ambosDto.getPromedioDiasRecepcion());
        assertEquals(2L, ambosDto.getRecibidasSinFecha());

        ProveedorRankingDTO soloOrdenesDto = resultado.get(1);
        assertEquals(0L, soloOrdenesDto.getCantidadValoraciones());
        assertNull(soloOrdenesDto.getPromedioTiempoEntregaValorado());
        assertEquals(2L, soloOrdenesDto.getOrdenesRecibidas());
        assertEquals(3.33, soloOrdenesDto.getPromedioDiasRecepcion());
        assertEquals(1L, soloOrdenesDto.getRecibidasSinFecha());

        ProveedorRankingDTO soloValoracionesDto = resultado.get(2);
        assertEquals(2L, soloValoracionesDto.getCantidadValoraciones());
        assertEquals(4.5, soloValoracionesDto.getPromedioTiempoEntregaValorado());
        assertEquals(0L, soloValoracionesDto.getOrdenesRecibidas());
        assertNull(soloValoracionesDto.getPromedioDiasRecepcion());
        assertEquals(0L, soloValoracionesDto.getRecibidasSinFecha());

        ProveedorRankingDTO sinActividadDto = resultado.get(3);
        assertFalse(sinActividadDto.getActivo());
        assertEquals(0L, sinActividadDto.getCantidadValoraciones());
        assertNull(sinActividadDto.getPromedioTiempoEntregaValorado());
        assertEquals(0L, sinActividadDto.getOrdenesRecibidas());
        assertNull(sinActividadDto.getPromedioDiasRecepcion());
        assertEquals(0L, sinActividadDto.getRecibidasSinFecha());
    }

    @Test
    void findProveedoresRanking_ordenaPorDiasConNullsAlFinalYLuegoValoraciones() {
        ReportRepository.ProveedorRankingBaseProjection proveedorA =
                proveedorRanking(1L, "Proveedor A", true);
        ReportRepository.ProveedorRankingBaseProjection proveedorB =
                proveedorRanking(2L, "Proveedor B", true);
        ReportRepository.ProveedorRankingBaseProjection proveedorC =
                proveedorRanking(3L, "Proveedor C", true);

        ReportRepository.ValoracionesProveedorProjection valoracionesA =
                valoraciones(1L, 1L, 5.0);
        ReportRepository.ValoracionesProveedorProjection valoracionesC =
                valoraciones(3L, 3L, 2.0);
        ReportRepository.OrdenesRecibidasProveedorProjection ordenesA =
                ordenes(1L, 1L, 2.0, 0L);
        ReportRepository.OrdenesRecibidasProveedorProjection ordenesB =
                ordenes(2L, 1L, 1.0, 0L);

        when(reportRepository.findProveedoresRankingBase(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0)))
                .thenReturn(List.of(proveedorA, proveedorB, proveedorC));
        when(reportRepository.findValoracionesPorProveedor(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0)))
                .thenReturn(List.of(valoracionesA, valoracionesC));
        when(reportRepository.findOrdenesRecibidasPorProveedor(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0)))
                .thenReturn(List.of(ordenesA, ordenesB));

        List<ProveedorRankingDTO> resultado = reportService.findProveedoresRanking(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertEquals(List.of(2L, 1L, 3L),
                resultado.stream().map(ProveedorRankingDTO::getIdProveedor).toList());
    }

    private ReportRepository.ProveedorRankingBaseProjection proveedorRanking(
            Long id, String razonSocial, boolean activo) {
        ReportRepository.ProveedorRankingBaseProjection fila =
                mock(ReportRepository.ProveedorRankingBaseProjection.class);
        when(fila.getIdProveedor()).thenReturn(id);
        when(fila.getRazonSocial()).thenReturn(razonSocial);
        when(fila.getActivo()).thenReturn(activo);
        return fila;
    }

    private ReportRepository.ValoracionesProveedorProjection valoraciones(
            Long id, Long cantidadValoraciones, Double promedioTiempoEntregaValorado) {
        ReportRepository.ValoracionesProveedorProjection fila =
                mock(ReportRepository.ValoracionesProveedorProjection.class);
        when(fila.getIdProveedor()).thenReturn(id);
        when(fila.getCantidadValoraciones()).thenReturn(cantidadValoraciones);
        when(fila.getPromedioTiempoEntregaValorado()).thenReturn(promedioTiempoEntregaValorado);
        return fila;
    }

    private ReportRepository.OrdenesRecibidasProveedorProjection ordenes(
            Long id, Long ordenesRecibidas, Double promedioDiasRecepcion, Long recibidasSinFecha) {
        ReportRepository.OrdenesRecibidasProveedorProjection fila =
                mock(ReportRepository.OrdenesRecibidasProveedorProjection.class);
        when(fila.getIdProveedor()).thenReturn(id);
        when(fila.getOrdenesRecibidas()).thenReturn(ordenesRecibidas);
        when(fila.getPromedioDiasRecepcion()).thenReturn(promedioDiasRecepcion);
        when(fila.getRecibidasSinFecha()).thenReturn(recibidasSinFecha);
        return fila;
    }

    private ReportRepository.UsuarioActividadBaseProjection usuario(
            Long id, String nombre, String apellido, String cuil, String rol, boolean activo) {
        ReportRepository.UsuarioActividadBaseProjection fila =
                mock(ReportRepository.UsuarioActividadBaseProjection.class);
        when(fila.getIdUsuario()).thenReturn(id);
        when(fila.getNombre()).thenReturn(nombre);
        when(fila.getApellido()).thenReturn(apellido);
        when(fila.getCuil()).thenReturn(cuil);
        when(fila.getRol()).thenReturn(rol);
        when(fila.getActivo()).thenReturn(activo);
        return fila;
    }

    private ReportRepository.RetirosUsuarioProjection retiros(
            Long id, Long cantidadRetiros, Long unidadesRetiradas, LocalDateTime ultimaActividad) {
        ReportRepository.RetirosUsuarioProjection fila =
                mock(ReportRepository.RetirosUsuarioProjection.class);
        when(fila.getIdUsuario()).thenReturn(id);
        when(fila.getCantidadRetiros()).thenReturn(cantidadRetiros);
        when(fila.getUnidadesRetiradas()).thenReturn(unidadesRetiradas);
        when(fila.getUltimaActividad()).thenReturn(ultimaActividad);
        return fila;
    }

    private ReportRepository.MovimientosUsuarioProjection movimientos(
            Long id, Long cantidadMovimientos, LocalDateTime ultimaActividad) {
        ReportRepository.MovimientosUsuarioProjection fila =
                mock(ReportRepository.MovimientosUsuarioProjection.class);
        when(fila.getIdUsuario()).thenReturn(id);
        when(fila.getCantidadMovimientos()).thenReturn(cantidadMovimientos);
        when(fila.getUltimaActividad()).thenReturn(ultimaActividad);
        return fila;
    }
}
