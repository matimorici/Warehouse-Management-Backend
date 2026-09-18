package big_three.wms.service;

import big_three.wms.dto.PurchaseOrderCreateDTO;
import big_three.wms.dto.PurchaseOrderLineCreateDTO;
import big_three.wms.dto.PurchaseOrderResponseDTO;
import big_three.wms.model.PurchaseOrder;
import big_three.wms.model.PurchaseOrderLine;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.PurchaseOrderLineRepository;
import big_three.wms.repository.PurchaseOrderRepository;
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
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderLineRepository purchaseOrderLineRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrderLineCreateDTO line(Long idProduct, int amount) {
        PurchaseOrderLineCreateDTO line = new PurchaseOrderLineCreateDTO();
        line.setIdProduct(idProduct);
        line.setAmount(amount);
        return line;
    }

    private PurchaseOrderCreateDTO orderDto(Long idSupplier, PurchaseOrderLineCreateDTO... lines) {
        PurchaseOrderCreateDTO dto = new PurchaseOrderCreateDTO();
        dto.setIdSupplier(idSupplier);
        dto.setLines(List.of(lines));
        return dto;
    }

    private PurchaseOrder order(Long id, PurchaseOrder.Status status) {
        PurchaseOrder order = new PurchaseOrder();
        order.setIdPurchaseOrder(id);
        order.setDateTime(LocalDateTime.now());
        order.setIdSupplier(1L);
        order.setStatus(status);
        return order;
    }

    private PurchaseOrderLine savedLine(Long idPurchaseOrder, Long idProduct, int amount) {
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setIdPurchaseOrder(idPurchaseOrder);
        line.setIdProduct(idProduct);
        line.setAmount(amount);
        return line;
    }

    private void stubSupplierAndProductsExist() {
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);
    }

    @Test
    void create_validOrder_savesOrderAndLines() {
        stubSupplierAndProductsExist();
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder order = inv.getArgument(0);
            order.setIdPurchaseOrder(10L);
            return order;
        });
        List<PurchaseOrderLine> lines = List.of(savedLine(10L, 1L, 2), savedLine(10L, 2L, 5));
        when(purchaseOrderLineRepository.findByIdPurchaseOrder(10L)).thenReturn(lines);

        PurchaseOrderResponseDTO response = purchaseOrderService.create(orderDto(1L, line(1L, 2), line(2L, 5)));

        assertEquals(10L, response.getIdPurchaseOrder());
        assertEquals(2, response.getLines().size());
        assertEquals("PENDIENTE", response.getStatus());
        assertNotNull(response.getDateTime());

        verify(purchaseOrderRepository).save(argThat(o -> o.getIdSupplier() == 1L && o.getStatus() == PurchaseOrder.Status.PENDIENTE));
        verify(purchaseOrderLineRepository).save(argThat(l -> l.getIdPurchaseOrder() == 10L && l.getIdProduct() == 1L));
        verify(purchaseOrderLineRepository).save(argThat(l -> l.getIdPurchaseOrder() == 10L && l.getIdProduct() == 2L));
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void create_unknownSupplier_throws() {
        when(proveedorRepository.existsById(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> purchaseOrderService.create(orderDto(9L, line(1L, 2))));

        assertEquals("Proveedor no encontrado", ex.getMessage());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void create_unknownProduct_throws() {
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> purchaseOrderService.create(orderDto(1L, line(9L, 2))));

        assertTrue(ex.getMessage().contains("9"));
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void findAllSummaries_returnsOrdersWithoutLines() {
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(order(1L, PurchaseOrder.Status.PENDIENTE)));

        List<PurchaseOrderResponseDTO> response = purchaseOrderService.findAllSummaries();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdPurchaseOrder());
        assertNull(response.get(0).getLines());
        verify(purchaseOrderLineRepository, never()).findByIdPurchaseOrder(any());
    }

    @Test
    void findById_includesLines() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order(1L, PurchaseOrder.Status.PENDIENTE)));
        when(purchaseOrderLineRepository.findByIdPurchaseOrder(1L)).thenReturn(List.of(savedLine(1L, 2L, 4)));

        PurchaseOrderResponseDTO response = purchaseOrderService.findById(1L);

        assertEquals(1, response.getLines().size());
        assertEquals(2L, response.getLines().get(0).getIdProduct());
        assertEquals(4, response.getLines().get(0).getAmount());
    }

    @Test
    void findById_notFound_throws() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> purchaseOrderService.findById(99L));

        assertEquals("Orden de compra no encontrada", ex.getMessage());
    }

    @Test
    void update_pendiente_replacesLinesWithoutStockAdjustment() {
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L, PurchaseOrder.Status.PENDIENTE)));
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);

        List<PurchaseOrderLine> oldLines = List.of(savedLine(10L, 1L, 3));
        List<PurchaseOrderLine> newLines = List.of(savedLine(10L, 2L, 4));
        when(purchaseOrderLineRepository.findByIdPurchaseOrder(10L)).thenReturn(oldLines, newLines);

        PurchaseOrderResponseDTO response = purchaseOrderService.update(10L, orderDto(1L, line(2L, 4)));

        verify(purchaseOrderLineRepository).deleteAll(oldLines);
        verify(purchaseOrderLineRepository).save(argThat(l -> l.getIdPurchaseOrder() == 10L && l.getIdProduct() == 2L));
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
        assertEquals(1, response.getLines().size());
        assertEquals(2L, response.getLines().get(0).getIdProduct());
    }

    @Test
    void update_recibida_reversesOldLinesAndAppliesNewDeltas() {
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L, PurchaseOrder.Status.RECIBIDA)));
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);

        List<PurchaseOrderLine> oldLines = List.of(savedLine(10L, 1L, 3));
        List<PurchaseOrderLine> newLines = List.of(savedLine(10L, 2L, 4));
        when(purchaseOrderLineRepository.findByIdPurchaseOrder(10L)).thenReturn(oldLines, newLines);

        PurchaseOrderResponseDTO response = purchaseOrderService.update(10L, orderDto(1L, line(2L, 4)));

        verify(productService).ajustarStock(1L, -3, 0);
        verify(purchaseOrderLineRepository).deleteAll(oldLines);
        verify(productService).ajustarStock(2L, 4, 0);
        assertEquals(1, response.getLines().size());
        assertEquals(2L, response.getLines().get(0).getIdProduct());
    }

    @Test
    void update_notFound_throws() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> purchaseOrderService.update(99L, orderDto(1L, line(1L, 2))));
        verify(purchaseOrderLineRepository, never()).save(any());
        verify(purchaseOrderLineRepository, never()).deleteAll(any());
    }

    @Test
    void receive_adjustsStockAndMarksReceived() {
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L, PurchaseOrder.Status.PENDIENTE)));
        List<PurchaseOrderLine> lines = List.of(savedLine(10L, 1L, 3), savedLine(10L, 2L, 5));
        when(purchaseOrderLineRepository.findByIdPurchaseOrder(10L)).thenReturn(lines, lines);

        PurchaseOrderResponseDTO response = purchaseOrderService.receive(10L);

        verify(productService).ajustarStock(1L, 3, 0);
        verify(productService).ajustarStock(2L, 5, 0);
        assertEquals("RECIBIDA", response.getStatus());
        assertEquals("RECIBIDA", purchaseOrderRepository.findById(10L).get().getStatus().name());
    }

    @Test
    void receive_alreadyReceived_throws() {
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L, PurchaseOrder.Status.RECIBIDA)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> purchaseOrderService.receive(10L));

        assertEquals("La orden ya fue recibida", ex.getMessage());
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void receive_cancelled_throws() {
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L, PurchaseOrder.Status.CANCELADA)));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> purchaseOrderService.receive(10L));

        assertEquals("No se puede recibir una orden cancelada", ex.getMessage());
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void receive_notFound_throws() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> purchaseOrderService.receive(99L));
        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void deleteById_recibida_revertsStockAndDeletesLinesAndOrder() {
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L, PurchaseOrder.Status.RECIBIDA)));
        List<PurchaseOrderLine> lines = List.of(savedLine(10L, 1L, 3), savedLine(10L, 2L, 5));
        when(purchaseOrderLineRepository.findByIdPurchaseOrder(10L)).thenReturn(lines, lines);

        purchaseOrderService.deleteById(10L);

        verify(productService).ajustarStock(1L, -3, 0);
        verify(productService).ajustarStock(2L, -5, 0);
        verify(purchaseOrderLineRepository).deleteAll(lines);
        verify(purchaseOrderRepository).deleteById(10L);
    }

    @Test
    void deleteById_pendiente_doesNotAdjustStock() {
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order(10L, PurchaseOrder.Status.PENDIENTE)));
        List<PurchaseOrderLine> lines = List.of(savedLine(10L, 1L, 3));
        when(purchaseOrderLineRepository.findByIdPurchaseOrder(10L)).thenReturn(lines, lines);

        purchaseOrderService.deleteById(10L);

        verify(productService, never()).ajustarStock(anyLong(), anyInt(), anyInt());
        verify(purchaseOrderLineRepository).deleteAll(lines);
        verify(purchaseOrderRepository).deleteById(10L);
    }

    @Test
    void deleteById_notFound_throws() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> purchaseOrderService.deleteById(99L));
        verify(purchaseOrderRepository, never()).deleteById(any());
    }
}