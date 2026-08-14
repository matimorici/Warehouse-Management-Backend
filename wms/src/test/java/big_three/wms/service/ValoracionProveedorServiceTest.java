package big_three.wms.service;

import big_three.wms.dto.ValoracionProveedorCreateDTO;
import big_three.wms.dto.ValoracionProveedorResponseDTO;
import big_three.wms.model.Proveedor;
import big_three.wms.model.ValoracionProveedor;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.ValoracionProveedorRepository;
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
class ValoracionProveedorServiceTest {

    @Mock
    private ValoracionProveedorRepository valoracionProveedorRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ValoracionProveedorService valoracionProveedorService;

    private ValoracionProveedorCreateDTO dto() {
        ValoracionProveedorCreateDTO dto = new ValoracionProveedorCreateDTO();
        dto.setIdProveedor(1L);
        dto.setTiempoEntrega(3);
        dto.setFormaEntrega("Presencial");
        dto.setRelacionPrecioCalidad("Buena");
        return dto;
    }

    private Proveedor proveedor() {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(1L);
        proveedor.setCuit("20-12345678-9");
        proveedor.setRazonSocial("Razon Social");
        return proveedor;
    }

    private ValoracionProveedor valoracion(Long id) {
        ValoracionProveedor v = new ValoracionProveedor();
        v.setIdValoracion(id);
        v.setProveedor(proveedor());
        v.setTiempoEntrega(3);
        v.setFormaEntrega("Presencial");
        v.setRelacionPrecioCalidad("Buena");
        return v;
    }

    @Test
    void create_success_returnsDto() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor()));
        when(valoracionProveedorRepository.save(any(ValoracionProveedor.class))).thenAnswer(inv -> {
            ValoracionProveedor saved = inv.getArgument(0);
            saved.setIdValoracion(1L);
            return saved;
        });

        ValoracionProveedorResponseDTO response = valoracionProveedorService.create(dto());

        assertEquals(1L, response.getIdValoracion());
        assertEquals(1L, response.getIdProveedor());
        assertEquals("Buena", response.getRelacionPrecioCalidad());
        assertNotNull(response.getFechaHora());
    }

    @Test
    void create_proveedorNotFound_throws() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> valoracionProveedorService.create(dto()));
        verify(valoracionProveedorRepository, never()).save(any());
    }

    @Test
    void create_mapsAllOptionalFields() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor()));
        when(valoracionProveedorRepository.save(any(ValoracionProveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ValoracionProveedorResponseDTO response = valoracionProveedorService.create(dto());

        assertEquals(3, response.getTiempoEntrega());
        assertEquals("Presencial", response.getFormaEntrega());
        assertEquals("Buena", response.getRelacionPrecioCalidad());
    }

    @Test
    void findAll_mapsToDtos() {
        when(valoracionProveedorRepository.findAll()).thenReturn(List.of(valoracion(1L)));

        List<ValoracionProveedorResponseDTO> response = valoracionProveedorService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdValoracion());
    }

    @Test
    void findById_success_returnsDto() {
        when(valoracionProveedorRepository.findById(1L)).thenReturn(Optional.of(valoracion(1L)));

        ValoracionProveedorResponseDTO response = valoracionProveedorService.findById(1L);

        assertEquals(1L, response.getIdValoracion());
        assertEquals(1L, response.getIdProveedor());
    }

    @Test
    void findById_notFound_throws() {
        when(valoracionProveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> valoracionProveedorService.findById(99L));
    }

    @Test
    void findByProveedor_success_returnsDtos() {
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(valoracionProveedorRepository.findByIdProveedor(1L)).thenReturn(List.of(valoracion(1L)));

        List<ValoracionProveedorResponseDTO> response = valoracionProveedorService.findByProveedor(1L);

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdProveedor());
    }

    @Test
    void findByProveedor_notFound_throws() {
        when(proveedorRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> valoracionProveedorService.findByProveedor(99L));
    }

    @Test
    void findByProveedor_noValoraciones_returnsEmptyList() {
        when(proveedorRepository.existsById(1L)).thenReturn(true);
        when(valoracionProveedorRepository.findByIdProveedor(1L)).thenReturn(List.of());

        List<ValoracionProveedorResponseDTO> response = valoracionProveedorService.findByProveedor(1L);

        assertTrue(response.isEmpty());
    }

    @Test
    void update_success_updatesFields() {
        when(valoracionProveedorRepository.findById(1L)).thenReturn(Optional.of(valoracion(1L)));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor()));
        when(valoracionProveedorRepository.save(any(ValoracionProveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ValoracionProveedorCreateDTO dto = dto();
        dto.setTiempoEntrega(7);
        ValoracionProveedorResponseDTO response = valoracionProveedorService.update(1L, dto);

        assertEquals(7, response.getTiempoEntrega());
    }

    @Test
    void update_notFound_throws() {
        when(valoracionProveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> valoracionProveedorService.update(99L, dto()));
        verify(valoracionProveedorRepository, never()).save(any());
    }

    @Test
    void update_proveedorNotFound_throws() {
        when(valoracionProveedorRepository.findById(1L)).thenReturn(Optional.of(valoracion(1L)));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> valoracionProveedorService.update(1L, dto()));
        verify(valoracionProveedorRepository, never()).save(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(valoracionProveedorRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> valoracionProveedorService.deleteById(99L));
        verify(valoracionProveedorRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_existing_deletes() {
        when(valoracionProveedorRepository.existsById(1L)).thenReturn(true);

        valoracionProveedorService.deleteById(1L);

        verify(valoracionProveedorRepository).deleteById(1L);
    }
}
