package big_three.wms.service;

import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
