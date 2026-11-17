package big_three.wms.service;

import big_three.wms.dto.ProveedorCreateDTO;
import big_three.wms.dto.ProveedorResponseDTO;
import big_three.wms.model.Proveedor;
import big_three.wms.model.PurchaseOrder;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.PurchaseOrderRepository;
import big_three.wms.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ProveedorService proveedorService;

    private ProveedorCreateDTO dto(String cuit) {
        ProveedorCreateDTO dto = new ProveedorCreateDTO();
        dto.setCuit(cuit);
        dto.setRazonSocial("Razon Social");
        dto.setTelefono("5555");
        dto.setMail("mail@test.com");
        dto.setDireccion("Calle 1");
        return dto;
    }

    private Proveedor proveedor(Long id, String cuit) {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(id);
        proveedor.setCuit(cuit);
        proveedor.setRazonSocial("Razon Social");
        return proveedor;
    }

    @Test
    void create_success_returnsDto() {
        when(proveedorRepository.existsByCuit("20-12345678-9")).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor saved = inv.getArgument(0);
            saved.setIdProveedor(1L);
            return saved;
        });

        ProveedorResponseDTO response = proveedorService.create(dto("20-12345678-9"));

        assertEquals(1L, response.getIdProveedor());
        assertEquals("20-12345678-9", response.getCuit());
        assertEquals("Razon Social", response.getRazonSocial());
        assertEquals("mail@test.com", response.getMail());
    }

    @Test
    void create_duplicateCuit_throws() {
        when(proveedorRepository.existsByCuit("20-12345678-9")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> proveedorService.create(dto("20-12345678-9")));

        assertTrue(ex.getMessage().contains("20-12345678-9"));
        assertTrue(ex.getMessage().contains("puede estar dado de baja"));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void findAll_returnsOnlyActiveProviders() {
        when(proveedorRepository.findByFechaBajaIsNull())
                .thenReturn(List.of(proveedor(1L, "20-12345678-9")));

        List<ProveedorResponseDTO> response = proveedorService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdProveedor());
        verify(proveedorRepository).findByFechaBajaIsNull();
        verify(proveedorRepository, never()).findAll();
    }

    @Test
    void findById_notFound_throws() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> proveedorService.findById(99L));
    }

    @Test
    void findById_deletedProviderIsTreatedAsNotFound() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> proveedorService.findById(1L));

        assertEquals("Proveedor no encontrado", ex.getMessage());
    }

    @Test
    void update_success_updatesFields() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(1L))
                .thenReturn(Optional.of(proveedor(1L, "20-12345678-9")));
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProveedorResponseDTO response = proveedorService.update(1L, dto("20-12345678-9"));

        assertEquals("20-12345678-9", response.getCuit());
        assertEquals("Calle 1", response.getDireccion());
    }

    @Test
    void update_duplicateCuitOnAnotherProveedor_throws() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(1L))
                .thenReturn(Optional.of(proveedor(1L, "20-11111111-1")));
        when(proveedorRepository.existsByCuit("20-12345678-9")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> proveedorService.update(1L, dto("20-12345678-9")));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> proveedorService.deleteById(99L));
        verify(proveedorRepository, never()).save(any());
        verify(proveedorRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_deletedProviderIsTreatedAsNotFound() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> proveedorService.deleteById(1L));

        assertEquals("Proveedor no encontrado para eliminar", ex.getMessage());
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void deleteById_withActiveProducts_throws() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(1L))
                .thenReturn(Optional.of(proveedor(1L, "20-12345678-9")));
        when(productRepository.existsByProveedor_IdProveedorAndFechaBajaIsNull(1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> proveedorService.deleteById(1L));

        assertEquals("No se puede dar de baja un proveedor con productos activos u órdenes pendientes",
                ex.getMessage());
        verify(proveedorRepository, never()).save(any());
        verify(currentUserProvider, never()).requireCurrentUserId();
    }

    @Test
    void deleteById_withPendingPurchaseOrders_throws() {
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(1L))
                .thenReturn(Optional.of(proveedor(1L, "20-12345678-9")));
        when(purchaseOrderRepository.existsByIdSupplierAndStatusAndDeletedDateTimeIsNull(
                1L, PurchaseOrder.Status.PENDIENTE)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> proveedorService.deleteById(1L));

        assertEquals("No se puede dar de baja un proveedor con productos activos u órdenes pendientes",
                ex.getMessage());
        verify(proveedorRepository, never()).save(any());
        verify(currentUserProvider, never()).requireCurrentUserId();
    }

    @Test
    void deleteById_withoutActiveProductsOrPendingOrders_setsFechaBajaAndSaves() {
        Proveedor proveedor = proveedor(1L, "20-12345678-9");
        when(proveedorRepository.findByIdProveedorAndFechaBajaIsNull(1L)).thenReturn(Optional.of(proveedor));
        when(currentUserProvider.requireCurrentUserId()).thenReturn(2L);

        proveedorService.deleteById(1L);

        assertNotNull(proveedor.getFechaBaja());
        assertEquals(2L, proveedor.getIdUsuarioBaja());
        verify(proveedorRepository).save(proveedor);
        verify(proveedorRepository, never()).deleteById(any());
    }
}
