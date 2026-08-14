package big_three.wms.service;

import big_three.wms.dto.UbicacionCreateDTO;
import big_three.wms.dto.UbicacionResponseDTO;
import big_three.wms.model.Ubicacion;
import big_three.wms.repository.UbicacionRepository;
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
class UbicacionServiceTest {

    @Mock
    private UbicacionRepository ubicacionRepository;

    @InjectMocks
    private UbicacionService ubicacionService;

    private UbicacionCreateDTO dto() {
        UbicacionCreateDTO dto = new UbicacionCreateDTO();
        dto.setNombreUbicacion("Estanteria A1");
        return dto;
    }

    private Ubicacion ubicacion(Long id) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setIdUbicacion(id);
        ubicacion.setNombreUbicacion("Estanteria A1");
        return ubicacion;
    }

    @Test
    void create_success_returnsDto() {
        when(ubicacionRepository.save(any(Ubicacion.class))).thenAnswer(inv -> {
            Ubicacion saved = inv.getArgument(0);
            saved.setIdUbicacion(1L);
            return saved;
        });

        UbicacionResponseDTO response = ubicacionService.create(dto());

        assertEquals(1L, response.getIdUbicacion());
        assertEquals("Estanteria A1", response.getNombreUbicacion());
    }

    @Test
    void findAll_mapsToDtos() {
        when(ubicacionRepository.findAll()).thenReturn(List.of(ubicacion(1L)));

        List<UbicacionResponseDTO> response = ubicacionService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdUbicacion());
    }

    @Test
    void findAll_empty_returnsEmptyList() {
        when(ubicacionRepository.findAll()).thenReturn(List.of());

        List<UbicacionResponseDTO> response = ubicacionService.findAll();

        assertTrue(response.isEmpty());
    }

    @Test
    void findById_success_returnsDto() {
        when(ubicacionRepository.findById(1L)).thenReturn(Optional.of(ubicacion(1L)));

        UbicacionResponseDTO response = ubicacionService.findById(1L);

        assertEquals(1L, response.getIdUbicacion());
    }

    @Test
    void findById_notFound_throws() {
        when(ubicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> ubicacionService.findById(99L));
    }

    @Test
    void update_success_updatesName() {
        when(ubicacionRepository.findById(1L)).thenReturn(Optional.of(ubicacion(1L)));
        when(ubicacionRepository.save(any(Ubicacion.class))).thenAnswer(inv -> inv.getArgument(0));

        UbicacionCreateDTO dto = dto();
        dto.setNombreUbicacion("Estanteria B2");
        UbicacionResponseDTO response = ubicacionService.update(1L, dto);

        assertEquals("Estanteria B2", response.getNombreUbicacion());
    }

    @Test
    void update_notFound_throws() {
        when(ubicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> ubicacionService.update(99L, dto()));
        verify(ubicacionRepository, never()).save(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(ubicacionRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> ubicacionService.deleteById(99L));
        verify(ubicacionRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_existing_deletes() {
        when(ubicacionRepository.existsById(1L)).thenReturn(true);

        ubicacionService.deleteById(1L);

        verify(ubicacionRepository).deleteById(1L);
    }
}
