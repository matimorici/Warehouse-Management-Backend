package big_three.wms.service;

import big_three.wms.dto.ProductCreateDTO;
import big_three.wms.dto.ProductResponseDTO;
import big_three.wms.dto.StockResponseDTO;
import big_three.wms.dto.StockUpdateDTO;
import big_three.wms.model.Product;
import big_three.wms.model.Product.OrigenCodigoBarras;
import big_three.wms.model.Proveedor;
import big_three.wms.model.Stock;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductService productService;

    private Proveedor proveedor(Long id) {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(id);
        return proveedor;
    }

    private Stock stock(Long idProducto, int disponible, int pendiente) {
        Stock stock = new Stock();
        stock.setIdProducto(idProducto);
        stock.setFechaHora(LocalDateTime.now());
        stock.setCantidadDisponible(disponible);
        stock.setCantidadPendiente(pendiente);
        return stock;
    }

    private ProductCreateDTO dto(String codigoBarras, Integer disponible, Integer pendiente) {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setNombreProducto("Producto Test");
        dto.setDescripcionProducto("Descripcion");
        dto.setCodigoBarras(codigoBarras);
        dto.setIdProveedor(1L);
        dto.setOrigenCodigoBarras("FABRICANTE");
        dto.setCantidadDisponible(disponible);
        dto.setCantidadPendiente(pendiente);
        return dto;
    }

    private Product savedProductMock(Long id, String codigoBarras) {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setIdProducto(id);
            return p;
        });
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));
        Product product = new Product();
        product.setIdProducto(id);
        product.setNombreProducto("Producto Test");
        product.setDescripcionProducto("Descripcion");
        product.setCodigoBarras(codigoBarras);
        product.setProveedor(proveedor(1L));
        product.setOrigenCodigoBarras(OrigenCodigoBarras.FABRICANTE);
        return product;
    }

    @Test
    void create_withBarcode_setsFabricanteAndStock() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productRepository.existsByCodigoBarras("779123")).thenReturn(false);
        savedProductMock(3L, "779123");

        ProductResponseDTO response = productService.create(dto("779123", null, null));

        assertEquals(3L, response.getIdProducto());
        assertEquals("779123", response.getCodigoBarras());
        assertEquals(OrigenCodigoBarras.FABRICANTE.name(), response.getOrigenCodigoBarras());
        assertEquals(0, response.getCantidadDisponible());
        assertEquals(0, response.getCantidadPendiente());

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertEquals(3L, captor.getValue().getIdProducto());
        assertEquals(0, captor.getValue().getCantidadDisponible());
        assertNotNull(captor.getValue().getFechaHora());
    }

    @Test
    void create_withBarcode_setsProvidedStockQuantities() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productRepository.existsByCodigoBarras("779123")).thenReturn(false);
        savedProductMock(3L, "779123");

        ProductResponseDTO response = productService.create(dto("779123", 10, 4));

        assertEquals(10, response.getCantidadDisponible());
        assertEquals(4, response.getCantidadPendiente());
    }

    @Test
    void create_blankBarcode_generatesInternalCode() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productRepository.obtenerSiguienteSecuencia()).thenReturn(42L);
        savedProductMock(3L, "INT-000042");

        ProductResponseDTO response = productService.create(dto("", null, null));

        assertEquals("INT-000042", response.getCodigoBarras());
        assertEquals(OrigenCodigoBarras.INTERNO.name(), response.getOrigenCodigoBarras());
    }

    @Test
    void create_nullBarcode_generatesInternalCode() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productRepository.obtenerSiguienteSecuencia()).thenReturn(7L);
        savedProductMock(3L, "INT-000007");

        ProductResponseDTO response = productService.create(dto(null, null, null));

        assertEquals("INT-000007", response.getCodigoBarras());
        assertEquals(OrigenCodigoBarras.INTERNO.name(), response.getOrigenCodigoBarras());
    }

    @Test
    void create_duplicateBarcode_throws() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productRepository.existsByCodigoBarras("779123")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productService.create(dto("779123", null, null)));

        assertTrue(ex.getMessage().contains("779123"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_proveedorNotFound_throws() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productService.create(dto("779123", null, null)));

        assertEquals("Proveedor no encontrado", ex.getMessage());
    }

    @Test
    void update_newBarcode_setsFabricanteAndUpdatesStock() {
        Product existing = new Product();
        existing.setIdProducto(1L);
        existing.setCodigoBarras("old");
        existing.setOrigenCodigoBarras(OrigenCodigoBarras.INTERNO);
        existing.setProveedor(proveedor(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productRepository.existsByCodigoBarras("new")).thenReturn(false);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock(1L, 5, 3)));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = productService.update(1L, dto("new", 9, 1));

        assertEquals("new", response.getCodigoBarras());
        assertEquals(OrigenCodigoBarras.FABRICANTE.name(), response.getOrigenCodigoBarras());
        assertEquals(9, response.getCantidadDisponible());
        assertEquals(1, response.getCantidadPendiente());
    }

    @Test
    void update_blankBarcode_keepsExistingBarcode() {
        Product existing = new Product();
        existing.setIdProducto(1L);
        existing.setCodigoBarras("old");
        existing.setOrigenCodigoBarras(OrigenCodigoBarras.FABRICANTE);
        existing.setProveedor(proveedor(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock(1L, 5, 3)));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = productService.update(1L, dto(null, null, null));

        assertEquals("old", response.getCodigoBarras());
        assertEquals(OrigenCodigoBarras.FABRICANTE.name(), response.getOrigenCodigoBarras());
        verify(productRepository, never()).existsByCodigoBarras(any());
    }

    @Test
    void update_duplicateBarcode_throws() {
        Product existing = new Product();
        existing.setIdProducto(1L);
        existing.setCodigoBarras("old");
        existing.setProveedor(proveedor(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productRepository.existsByCodigoBarras("taken")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productService.update(1L, dto("taken", null, null)));
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_missingStock_upsertsWithZeros() {
        Product existing = new Product();
        existing.setIdProducto(1L);
        existing.setCodigoBarras("old");
        existing.setProveedor(proveedor(1L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(stockRepository.findById(1L)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = productService.update(1L, dto(null, 6, 2));

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getIdProducto());
        assertEquals(6, captor.getValue().getCantidadDisponible());
        assertEquals(2, captor.getValue().getCantidadPendiente());
        assertEquals(6, response.getCantidadDisponible());
    }

    @Test
    void deleteById_notFound_throws() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> productService.deleteById(99L));
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_deletesProductButNotStock() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteById(1L);

        verify(productRepository).deleteById(1L);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void ajustarStock_appliesDeltas() {
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock(1L, 5, 3)));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.ajustarStock(1L, -2, 2);

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getCantidadDisponible());
        assertEquals(5, captor.getValue().getCantidadPendiente());
    }

    @Test
    void ajustarStock_negativeDisponible_throws() {
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock(1L, 1, 0)));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> productService.ajustarStock(1L, -2, 2));

        assertTrue(ex.getMessage().contains("Stock disponible insuficiente"));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void ajustarStock_missingStock_throws() {
        when(stockRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.ajustarStock(1L, -1, 1));
    }

    @Test
    void findStockByIdProducto_missing_throws() {
        when(stockRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.findStockByIdProducto(1L));
    }

    @Test
    void updateStock_appliesOnlyProvidedFields() {
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock(1L, 5, 3)));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setCantidadDisponible(9);

        StockResponseDTO response = productService.updateStock(1L, dto);

        assertEquals(9, response.getCantidadDisponible());
        assertEquals(3, response.getCantidadPendiente());
    }

    @Test
    void findAll_includesStock() {
        Product product = new Product();
        product.setIdProducto(1L);
        product.setCodigoBarras("779123");
        product.setProveedor(proveedor(1L));
        product.setOrigenCodigoBarras(OrigenCodigoBarras.FABRICANTE);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock(1L, 5, 3)));

        List<ProductResponseDTO> response = productService.findAll();

        assertEquals(1, response.size());
        assertEquals(5, response.get(0).getCantidadDisponible());
        assertEquals(3, response.get(0).getCantidadPendiente());
    }

    @Test
    void findById_notFound_throws() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.findById(1L));
    }
}
