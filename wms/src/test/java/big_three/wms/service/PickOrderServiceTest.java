package big_three.wms.service;

import big_three.wms.dto.PickOrderCreateDTO;
import big_three.wms.dto.PickOrderLineCreateDTO;
import big_three.wms.dto.PickOrderResponseDTO;
import big_three.wms.model.PickOrder;
import big_three.wms.model.PickOrderLine;
import big_three.wms.repository.PickOrderLineRepository;
import big_three.wms.repository.PickOrderRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickOrderServiceTest {

    @Mock
    private PickOrderRepository pickOrderRepository;

    @Mock
    private PickOrderLineRepository pickOrderLineRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private PickOrderService pickOrderService;

    private PickOrderLineCreateDTO line(Long idProducto, int cantidad) {
        PickOrderLineCreateDTO line = new PickOrderLineCreateDTO();
        line.setIdProducto(idProducto);
        line.setCantidad(cantidad);
        return line;
    }

    private PickOrderCreateDTO orderDto(Long idUsuario, PickOrderLineCreateDTO... lines) {
        PickOrderCreateDTO dto = new PickOrderCreateDTO();
        dto.setIdUsuario(idUsuario);
        dto.setLineasRetiro(List.of(lines));
        return dto;
    }

    private PickOrderLine savedLine(Long idOrdenRetiro, Long idProducto, int cantidad) {
        PickOrderLine line = new PickOrderLine();
        line.setIdOrdenRetiro(idOrdenRetiro);
        line.setIdProducto(idProducto);
        line.setCantidad(cantidad);
        return line;
    }

    private void stubUserAndProductsExist() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);
    }

    @Test
    void create_validOrder_savesOrderAndLinesAndAdjustsStock() {
        stubUserAndProductsExist();
        when(pickOrderRepository.save(any(PickOrder.class))).thenAnswer(inv -> {
            PickOrder order = inv.getArgument(0);
            order.setIdOrdenRetiro(10L);
            return order;
        });
        List<PickOrderLine> lines = List.of(savedLine(10L, 1L, 2), savedLine(10L, 2L, 5));
        when(pickOrderLineRepository.findByIdOrdenRetiro(10L)).thenReturn(lines);

        PickOrderResponseDTO response = pickOrderService.create(orderDto(1L, line(1L, 2), line(2L, 5)));

        assertEquals(10L, response.getIdOrdenRetiro());
        assertEquals(2, response.getLineasRetiro().size());
        assertNotNull(response.getFechaHora());

        verify(pickOrderLineRepository).save(argThat(l -> l.getIdOrdenRetiro() == 10L && l.getIdProducto() == 1L));
        verify(pickOrderLineRepository).save(argThat(l -> l.getIdOrdenRetiro() == 10L && l.getIdProducto() == 2L));
        verify(productService).ajustarStock(1L, -2, 2);
        verify(productService).ajustarStock(2L, -5, 5);
    }

    @Test
    void create_unknownUser_throws() {
        when(userRepository.existsById(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.create(orderDto(9L, line(1L, 2))));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(pickOrderRepository, never()).save(any());
    }

    @Test
    void create_unknownProduct_throws() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.create(orderDto(1L, line(9L, 2))));

        assertTrue(ex.getMessage().contains("9"));
        verify(pickOrderRepository, never()).save(any());
    }

    @Test
    void update_reversesOldLinesAndAppliesNewDeltas() {
        PickOrder order = new PickOrder();
        order.setIdOrdenRetiro(10L);
        order.setFechaHora(LocalDateTime.now());
        order.setIdUsuario(1L);
        when(pickOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);

        List<PickOrderLine> oldLines = List.of(savedLine(10L, 1L, 3));
        List<PickOrderLine> newLines = List.of(savedLine(10L, 2L, 4));
        when(pickOrderLineRepository.findByIdOrdenRetiro(10L)).thenReturn(oldLines, newLines);

        PickOrderResponseDTO response = pickOrderService.update(10L, orderDto(1L, line(2L, 4)));

        verify(productService).ajustarStock(1L, 3, -3);
        verify(pickOrderLineRepository).deleteAll(oldLines);
        verify(productService).ajustarStock(2L, -4, 4);
        assertEquals(1, response.getLineasRetiro().size());
        assertEquals(2L, response.getLineasRetiro().get(0).getIdProducto());
    }

    @Test
    void update_notFound_throws() {
        when(pickOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> pickOrderService.update(99L, orderDto(1L, line(1L, 2))));
        verify(pickOrderLineRepository, never()).save(any());
    }

    @Test
    void deleteById_revertsStockAndDeletesLinesAndOrder() {
        when(pickOrderRepository.existsById(10L)).thenReturn(true);
        List<PickOrderLine> lines = List.of(savedLine(10L, 1L, 3), savedLine(10L, 2L, 5));
        when(pickOrderLineRepository.findByIdOrdenRetiro(10L)).thenReturn(lines);

        pickOrderService.deleteById(10L);

        verify(productService).ajustarStock(1L, 3, -3);
        verify(productService).ajustarStock(2L, 5, -5);
        verify(pickOrderLineRepository).deleteAll(lines);
        verify(pickOrderRepository).deleteById(10L);
    }

    @Test
    void deleteById_notFound_throws() {
        when(pickOrderRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> pickOrderService.deleteById(99L));
        verify(pickOrderRepository, never()).deleteById(any());
    }

    @Test
    void findAllSummaries_returnsOrdersWithoutLines() {
        PickOrder order = new PickOrder();
        order.setIdOrdenRetiro(1L);
        order.setFechaHora(LocalDateTime.now());
        order.setIdUsuario(1L);
        when(pickOrderRepository.findAll()).thenReturn(List.of(order));

        List<PickOrderResponseDTO> response = pickOrderService.findAllSummaries();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdOrdenRetiro());
        assertNull(response.get(0).getLineasRetiro());
        verify(pickOrderLineRepository, never()).findByIdOrdenRetiro(any());
    }

    @Test
    void findById_includesLines() {
        PickOrder order = new PickOrder();
        order.setIdOrdenRetiro(1L);
        order.setFechaHora(LocalDateTime.now());
        order.setIdUsuario(1L);
        when(pickOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(pickOrderLineRepository.findByIdOrdenRetiro(1L)).thenReturn(List.of(savedLine(1L, 2L, 4)));

        PickOrderResponseDTO response = pickOrderService.findById(1L);

        assertEquals(1, response.getLineasRetiro().size());
        assertEquals(2L, response.getLineasRetiro().get(0).getIdProducto());
        assertEquals(4, response.getLineasRetiro().get(0).getCantidad());
    }

    @Test
    void findById_notFound_throws() {
        when(pickOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> pickOrderService.findById(99L));
    }
}
