package big_three.wms.service;

import big_three.wms.dto.MovimientoFisicoCreateDTO;
import big_three.wms.dto.MovimientoFisicoResponseDTO;
import big_three.wms.model.MovimientoFisico;
import big_three.wms.model.MovimientoFisico.MovimientoFisicoId;
import big_three.wms.repository.MovimientoFisicoRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.UbicacionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoFisicoServiceTest {

    @Mock
    private MovimientoFisicoRepository movimientoFisicoRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MovimientoFisicoService movimientoFisicoService;

    private MovimientoFisicoCreateDTO dto() {
        MovimientoFisicoCreateDTO dto = new MovimientoFisicoCreateDTO();
        dto.setIdProducto(1L);
        dto.setIdUbicacionDesde(2L);
        dto.setIdUbicacionHasta(3L);
        dto.setIdUsuario(4L);
        return dto;
    }

    private MovimientoFisico movimiento(Long idProducto) {
        MovimientoFisico m = new MovimientoFisico();
        m.setIdProducto(idProducto);
        m.setFechaHora(LocalDateTime.now());
        m.setIdUbicacionDesde(2L);
        m.setIdUbicacionHasta(3L);
        m.setIdUsuario(4L);
        return m;
    }

    private void stubAllExist() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(ubicacionRepository.existsById(3L)).thenReturn(true);
        when(ubicacionRepository.existsById(2L)).thenReturn(true);
        when(userRepository.existsById(4L)).thenReturn(true);
    }

    @Test
    void create_success_returnsDto() {
        stubAllExist();
        when(movimientoFisicoRepository.save(any(MovimientoFisico.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoFisicoResponseDTO response = movimientoFisicoService.create(dto());

        assertEquals(1L, response.getIdProducto());
        assertEquals(2L, response.getIdUbicacionDesde());
        assertEquals(3L, response.getIdUbicacionHasta());
        assertEquals(4L, response.getIdUsuario());
        assertNotNull(response.getFechaHora());
    }

    @Test
    void create_productNotFound_throws() {
        when(productRepository.existsById(1L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> movimientoFisicoService.create(dto()));
        verify(movimientoFisicoRepository, never()).save(any());
    }

    @Test
    void create_ubicacionDestinoNotFound_throws() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(ubicacionRepository.existsById(3L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> movimientoFisicoService.create(dto()));
        verify(movimientoFisicoRepository, never()).save(any());
    }

    @Test
    void create_ubicacionOrigenNotFound_throws() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(ubicacionRepository.existsById(3L)).thenReturn(true);
        when(ubicacionRepository.existsById(2L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> movimientoFisicoService.create(dto()));
        verify(movimientoFisicoRepository, never()).save(any());
    }

    @Test
    void create_usuarioNotFound_throws() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(ubicacionRepository.existsById(3L)).thenReturn(true);
        when(ubicacionRepository.existsById(2L)).thenReturn(true);
        when(userRepository.existsById(4L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> movimientoFisicoService.create(dto()));
        verify(movimientoFisicoRepository, never()).save(any());
    }

    @Test
    void findAll_mapsToDtos() {
        when(movimientoFisicoRepository.findAll()).thenReturn(List.of(movimiento(1L)));

        List<MovimientoFisicoResponseDTO> response = movimientoFisicoService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdProducto());
    }

    @Test
    void findByProducto_returnsDtos() {
        when(movimientoFisicoRepository.findByIdProducto(1L)).thenReturn(List.of(movimiento(1L)));

        List<MovimientoFisicoResponseDTO> response = movimientoFisicoService.findByProducto(1L);

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdProducto());
    }

    @Test
    void findByProducto_noMovimientos_returnsEmptyList() {
        when(movimientoFisicoRepository.findByIdProducto(1L)).thenReturn(List.of());

        List<MovimientoFisicoResponseDTO> response = movimientoFisicoService.findByProducto(1L);

        assertTrue(response.isEmpty());
    }

    @Test
    void findByIdProductoAndFechaHora_success_returnsDto() {
        LocalDateTime fecha = LocalDateTime.now();
        when(movimientoFisicoRepository.findById(new MovimientoFisicoId(1L, fecha)))
                .thenReturn(Optional.of(movimiento(1L)));

        MovimientoFisicoResponseDTO response = movimientoFisicoService.findByIdProductoAndFechaHora(1L, fecha);

        assertEquals(1L, response.getIdProducto());
    }

    @Test
    void findByIdProductoAndFechaHora_notFound_throws() {
        LocalDateTime fecha = LocalDateTime.now();
        when(movimientoFisicoRepository.findById(new MovimientoFisicoId(1L, fecha)))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> movimientoFisicoService.findByIdProductoAndFechaHora(1L, fecha));
    }
}
