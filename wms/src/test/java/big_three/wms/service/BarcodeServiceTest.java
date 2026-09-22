package big_three.wms.service;

import big_three.wms.model.Product;
import big_three.wms.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarcodeServiceTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BarcodeService barcodeService;

    private Product product(String codigoBarras) {
        Product product = new Product();
        product.setIdProducto(1L);
        product.setCodigoBarras(codigoBarras);
        return product;
    }

    @Test
    void getBarcodePng_productFound_returnsPngBytes() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("779123")));

        byte[] png = barcodeService.getBarcodePng(1L);

        assertNotNull(png);
        assertTrue(png.length > 0);
        assertArrayEquals(PNG_SIGNATURE, java.util.Arrays.copyOf(png, PNG_SIGNATURE.length));
    }

    @Test
    void getBarcodePng_productNotFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> barcodeService.getBarcodePng(99L));

        assertEquals("Producto no encontrado", ex.getMessage());
    }
}