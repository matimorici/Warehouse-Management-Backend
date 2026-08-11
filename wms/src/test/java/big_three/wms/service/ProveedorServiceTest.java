package big_three.wms.service;

import big_three.wms.dto.ProveedorCreateDTO;
import big_three.wms.dto.ProveedorResponseDTO;
import big_three.wms.model.Proveedor;
import big_three.wms.repository.ProveedorRepository;
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
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void findAll_mapsToDtos() {
        when(proveedorRepository.findAll()).thenReturn(List.of(proveedor(1L, "20-12345678-9")));

        List<ProveedorResponseDTO> response = proveedorService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdProveedor());
    }

    @Test
    void findById_notFound_throws() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> proveedorService.findById(99L));
    }

    @Test
    void update_success_updatesFields() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L, "20-12345678-9")));
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProveedorResponseDTO response = proveedorService.update(1L, dto("20-12345678-9"));

        assertEquals("20-12345678-9", response.getCuit());
        assertEquals("Calle 1", response.getDireccion());
    }

    @Test
    void update_duplicateCuitOnAnotherProveedor_throws() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L, "20-11111111-1")));
        when(proveedorRepository.existsByCuit("20-12345678-9")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> proveedorService.update(1L, dto("20-12345678-9")));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(proveedorRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> proveedorService.deleteById(99L));
        verify(proveedorRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_existing_deletes() {
        when(proveedorRepository.existsById(1L)).thenReturn(true);

        proveedorService.deleteById(1L);

        verify(proveedorRepository).deleteById(1L);
    }
}
