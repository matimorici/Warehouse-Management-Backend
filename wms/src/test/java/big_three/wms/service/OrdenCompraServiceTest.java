package big_three.wms.service;

import big_three.wms.dto.LineaCompraCreateDTO;
import big_three.wms.dto.OrdenCompraCreateDTO;
import big_three.wms.dto.OrdenCompraResponseDTO;
import big_three.wms.model.LineaCompra;
import big_three.wms.model.OrdenCompra;
import big_three.wms.model.OrdenCompra.EstadoOrdenCompra;
import big_three.wms.repository.LineaCompraRepository;
import big_three.wms.repository.OrdenCompraRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private LineaCompraRepository lineaCompraRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrdenCompraService ordenCompraService;

    private LineaCompraCreateDTO line(Long idProducto, int cantidad) {
        LineaCompraCreateDTO line = new LineaCompraCreateDTO();
        line.setIdProducto(idProducto);
        line.setCantidad(cantidad);
        return line;
    }

    private OrdenCompraCreateDTO dto(Long idProveedor, LineaCompraCreateDTO... lines) {
        OrdenCompraCreateDTO dto = new OrdenCompraCreateDTO();
        dto.setIdProveedor(idProveedor);
        dto.setLineasCompra(List.of(lines));
        return dto;
    }

    private LineaCompra savedLine(Long idOrdenCompra, Long idProducto, int cantidad) {
        LineaCompra line = new LineaCompra();
        line.setIdOrdenCompra(idOrdenCompra);
        line.setIdProducto(idProducto);
        line.setCantidad(cantidad);
        return line;
    }

    private OrdenCompra orden(Long id, EstadoOrdenCompra estado) {
        OrdenCompra orden = new OrdenCompra();
        orden.setIdOrdenCompra(id);
        orden.setFechaHora(LocalDateTime.now());
        orden.setIdProveedor(1L);
        orden.setEstado(estado);
        return orden;
    }

    private void stubProveedorAndProductsExist() {
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);
    }

    @Test
    void create_validOrder_savesOrderAndLinesAsPendiente() {
        stubProveedorAndProductsExist();
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(inv -> {
            OrdenCompra orden = inv.getArgument(0);
            orden.setIdOrdenCompra(10L);
            return orden;
        });
        List<LineaCompra> lines = List.of(savedLine(10L, 1L, 2), savedLine(10L, 2L, 5));
        when(lineaCompraRepository.findByIdOrdenCompra(10L)).thenReturn(lines);

        OrdenCompraResponseDTO response = ordenCompraService.create(dto(1L, line(1L, 2), line(2L, 5)));

        assertEquals(10L, response.getIdOrdenCompra());
        assertEquals("PENDIENTE", response.getEstado());
        assertEquals(2, response.getLineasCompra().size());
        assertNotNull(response.getFechaHora());
        verify(lineaCompraRepository).save(argThat(l -> l.getIdOrdenCompra() == 10L && l.getIdProducto() == 1L));
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void create_unknownProveedor_throws() {
        when(proveedorRepository.existsById(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ordenCompraService.create(dto(9L, line(1L, 2))));

        assertEquals("Proveedor no encontrado", ex.getMessage());
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    void create_unknownProduct_throws() {
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ordenCompraService.create(dto(1L, line(9L, 2))));

        assertTrue(ex.getMessage().contains("9"));
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    void update_toRecibida_adjustsStock() {
        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(orden(10L, EstadoOrdenCompra.PENDIENTE)));
        stubProveedorAndProductsExist();
        List<LineaCompra> newLines = List.of(savedLine(10L, 1L, 4), savedLine(10L, 2L, 3));
        when(lineaCompraRepository.findByIdOrdenCompra(10L)).thenReturn(List.of(), newLines);

        OrdenCompraResponseDTO response = ordenCompraService.update(10L, dto(1L, line(1L, 4), line(2L, 3)), "RECIBIDA");

        assertEquals("RECIBIDA", response.getEstado());
        verify(productService).ajustarStock(1L, 4, 0);
        verify(productService).ajustarStock(2L, 3, 0);
    }

    @Test
    void update_toCancelada_doesNotAdjustStock() {
        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(orden(10L, EstadoOrdenCompra.PENDIENTE)));
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        List<LineaCompra> newLines = List.of(savedLine(10L, 1L, 4));
        when(lineaCompraRepository.findByIdOrdenCompra(10L)).thenReturn(List.of(), newLines);

        OrdenCompraResponseDTO response = ordenCompraService.update(10L, dto(1L, line(1L, 4)), "CANCELADA");

        assertEquals("CANCELADA", response.getEstado());
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void update_recibidaOrder_throws() {
        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(orden(10L, EstadoOrdenCompra.RECIBIDA)));

        assertThrows(IllegalArgumentException.class,
                () -> ordenCompraService.update(10L, dto(1L, line(1L, 4)), "CANCELADA"));
        verify(lineaCompraRepository, never()).deleteAll(any());
    }

    @Test
    void update_invalidEstado_throws() {
        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(orden(10L, EstadoOrdenCompra.PENDIENTE)));
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> ordenCompraService.update(10L, dto(1L, line(1L, 4)), "OTRO"));
    }

    @Test
    void update_notFound_throws() {
        when(ordenCompraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> ordenCompraService.update(99L, dto(1L, line(1L, 2)), "PENDIENTE"));
        verify(lineaCompraRepository, never()).save(any());
    }

    @Test
    void deleteById_pendiente_deletesLinesAndOrder() {
        when(ordenCompraRepository.existsById(10L)).thenReturn(true);
        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(orden(10L, EstadoOrdenCompra.PENDIENTE)));
        List<LineaCompra> lines = List.of(savedLine(10L, 1L, 3));
        when(lineaCompraRepository.findByIdOrdenCompra(10L)).thenReturn(lines);

        ordenCompraService.deleteById(10L);

        verify(lineaCompraRepository).deleteAll(lines);
        verify(ordenCompraRepository).deleteById(10L);
    }

    @Test
    void deleteById_recibida_throws() {
        when(ordenCompraRepository.existsById(10L)).thenReturn(true);
        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(orden(10L, EstadoOrdenCompra.RECIBIDA)));

        assertThrows(IllegalArgumentException.class, () -> ordenCompraService.deleteById(10L));
        verify(ordenCompraRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(ordenCompraRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> ordenCompraService.deleteById(99L));
        verify(ordenCompraRepository, never()).deleteById(any());
    }

    @Test
    void findAllSummaries_returnsOrdersWithoutLines() {
        when(ordenCompraRepository.findAll()).thenReturn(List.of(orden(1L, EstadoOrdenCompra.PENDIENTE)));

        List<OrdenCompraResponseDTO> response = ordenCompraService.findAllSummaries();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdOrdenCompra());
        assertNull(response.get(0).getLineasCompra());
        verify(lineaCompraRepository, never()).findByIdOrdenCompra(any());
    }

    @Test
    void findById_includesLines() {
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(orden(1L, EstadoOrdenCompra.PENDIENTE)));
        when(lineaCompraRepository.findByIdOrdenCompra(1L)).thenReturn(List.of(savedLine(1L, 2L, 4)));

        OrdenCompraResponseDTO response = ordenCompraService.findById(1L);

        assertEquals(1, response.getLineasCompra().size());
        assertEquals(2L, response.getLineasCompra().get(0).getIdProducto());
        assertEquals(4, response.getLineasCompra().get(0).getCantidad());
    }

    @Test
    void findById_notFound_throws() {
        when(ordenCompraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> ordenCompraService.findById(99L));
    }
}
