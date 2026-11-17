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
import big_three.wms.security.CurrentUserProvider;
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

    @Mock
    private CurrentUserProvider currentUserProvider;

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

    private PickOrder order(Long id) {
        PickOrder order = new PickOrder();
        order.setIdOrdenRetiro(id);
        order.setFechaHora(LocalDateTime.now());
        order.setIdUsuario(1L);
        return order;
    }

    private PickOrder deletedOrder(Long id) {
        PickOrder order = order(id);
        order.setFechaBaja(LocalDateTime.now().minusDays(1));
        order.setIdUsuarioBaja(3L);
        return order;
    }

    private void stubUserAndProductsExist() {
        when(userRepository.existsByIdUsuarioAndFechaBajaIsNull(1L)).thenReturn(true);
        when(productRepository.existsByIdProductoAndFechaBajaIsNull(1L)).thenReturn(true);
        when(productRepository.existsByIdProductoAndFechaBajaIsNull(2L)).thenReturn(true);
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
        when(userRepository.existsByIdUsuarioAndFechaBajaIsNull(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.create(orderDto(9L, line(1L, 2))));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(pickOrderRepository, never()).save(any());
    }

    @Test
    void create_unknownProduct_throws() {
        when(userRepository.existsByIdUsuarioAndFechaBajaIsNull(1L)).thenReturn(true);
        when(productRepository.existsByIdProductoAndFechaBajaIsNull(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.create(orderDto(1L, line(9L, 2))));

        assertTrue(ex.getMessage().contains("9"));
        verify(pickOrderRepository, never()).save(any());
    }

    @Test
    void create_deletedUserIsTreatedAsNotFound() {
        when(userRepository.existsByIdUsuarioAndFechaBajaIsNull(1L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.create(orderDto(1L, line(1L, 2))));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(pickOrderRepository, never()).save(any());
    }

    @Test
    void create_deletedProductIsTreatedAsNotFound() {
        when(userRepository.existsByIdUsuarioAndFechaBajaIsNull(1L)).thenReturn(true);
        when(productRepository.existsByIdProductoAndFechaBajaIsNull(1L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.create(orderDto(1L, line(1L, 2))));

        assertEquals("Producto no encontrado: 1", ex.getMessage());
        verify(pickOrderRepository, never()).save(any());
    }

    @Test
    void update_reversesOldLinesAndAppliesNewDeltas() {
        when(pickOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L)));
        when(userRepository.existsByIdUsuarioAndFechaBajaIsNull(1L)).thenReturn(true);
        when(productRepository.existsByIdProductoAndFechaBajaIsNull(2L)).thenReturn(true);

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
    void update_deleted_throws() {
        when(pickOrderRepository.findById(10L)).thenReturn(Optional.of(deletedOrder(10L)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.update(10L, orderDto(1L, line(1L, 2))));

        assertEquals("Orden de retiro no encontrada", ex.getMessage());
        verify(pickOrderRepository, never()).save(any());
        verify(pickOrderLineRepository, never()).save(any());
    }

    @Test
    void deleteById_revertsStockAndMarksDeleted() {
        PickOrder order = order(10L);
        when(pickOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        List<PickOrderLine> lines = List.of(savedLine(10L, 1L, 3), savedLine(10L, 2L, 5));
        when(pickOrderLineRepository.findByIdOrdenRetiro(10L)).thenReturn(lines);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(7L);

        pickOrderService.deleteById(10L);

        verify(productService).ajustarStock(1L, 3, -3);
        verify(productService).ajustarStock(2L, 5, -5);
        assertNotNull(order.getFechaBaja());
        assertEquals(7L, order.getIdUsuarioBaja());
        verify(pickOrderRepository).save(order);
        verify(pickOrderLineRepository, never()).deleteAll(any());
        verify(pickOrderRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(pickOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> pickOrderService.deleteById(99L));
        verify(pickOrderRepository, never()).deleteById(any());
        verify(currentUserProvider, never()).requireCurrentUserId();
    }

    @Test
    void deleteById_alreadyDeleted_throwsWithoutTouchingStock() {
        when(pickOrderRepository.findById(10L)).thenReturn(Optional.of(deletedOrder(10L)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pickOrderService.deleteById(10L));

        assertEquals("Orden de retiro no encontrada", ex.getMessage());
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
        verify(pickOrderRepository, never()).save(any());
        verify(currentUserProvider, never()).requireCurrentUserId();
    }

    @Test
    void findAllSummaries_returnsActiveOrdersWithoutLines() {
        when(pickOrderRepository.findByFechaBajaIsNull()).thenReturn(List.of(order(1L)));

        List<PickOrderResponseDTO> response = pickOrderService.findAllSummaries();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdOrdenRetiro());
        assertNull(response.get(0).getLineasRetiro());
        verify(pickOrderRepository).findByFechaBajaIsNull();
        verify(pickOrderRepository, never()).findAll();
        verify(pickOrderLineRepository, never()).findByIdOrdenRetiro(any());
    }

    @Test
    void findById_includesLines() {
        when(pickOrderRepository.findById(1L)).thenReturn(Optional.of(order(1L)));
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

    @Test
    void findById_deleted_throws() {
        when(pickOrderRepository.findById(10L)).thenReturn(Optional.of(deletedOrder(10L)));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> pickOrderService.findById(10L));

        assertEquals("Orden de retiro no encontrada", ex.getMessage());
        verify(pickOrderLineRepository, never()).findByIdOrdenRetiro(any());
    }
}
