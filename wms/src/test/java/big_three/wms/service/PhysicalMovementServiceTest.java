package big_three.wms.service;

import big_three.wms.dto.PhysicalMovementCreateDTO;
import big_three.wms.dto.PhysicalMovementResponseDTO;
import big_three.wms.model.Location;
import big_three.wms.model.PhysicalMovement;
import big_three.wms.model.Product;
import big_three.wms.model.User;
import big_three.wms.repository.LocationRepository;
import big_three.wms.repository.PhysicalMovementRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.UserRepository;
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
class PhysicalMovementServiceTest {

    @Mock
    private PhysicalMovementRepository movementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PhysicalMovementService movementService;

    private PhysicalMovementCreateDTO dto() {
        PhysicalMovementCreateDTO dto = new PhysicalMovementCreateDTO();
        dto.setIdProduct(1L);
        dto.setIdLocationFrom(10L);
        dto.setIdLocationTo(20L);
        dto.setIdUser(30L);
        return dto;
    }

    private PhysicalMovement movement(LocalDateTime dateTime) {
        PhysicalMovement movement = new PhysicalMovement();
        movement.setIdProduct(1L);
        movement.setDateTime(dateTime);
        movement.setIdLocationFrom(10L);
        movement.setIdLocationTo(20L);
        movement.setIdUser(30L);
        return movement;
    }

    @Test
    void create_success_returnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(new Location()));
        when(locationRepository.findById(20L)).thenReturn(Optional.of(new Location()));
        when(userRepository.findById(30L)).thenReturn(Optional.of(new User()));
        when(movementRepository.save(any(PhysicalMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        PhysicalMovementResponseDTO response = movementService.create(dto());

        assertEquals(1L, response.getIdProduct());
        assertNotNull(response.getDateTime());
        assertEquals(10L, response.getIdLocationFrom());
        assertEquals(20L, response.getIdLocationTo());
        assertEquals(30L, response.getIdUser());

        ArgumentCaptor<PhysicalMovement> captor = ArgumentCaptor.forClass(PhysicalMovement.class);
        verify(movementRepository).save(captor.capture());
        assertNotNull(captor.getValue().getDateTime());
        assertEquals(1L, captor.getValue().getIdProduct());
    }

    @Test
    void create_productNotFound_throws() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> movementService.create(dto()));

        assertEquals("Product not found", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void create_sourceLocationNotFound_throws() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));
        when(locationRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> movementService.create(dto()));

        assertEquals("Source location not found", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void create_withoutSourceLocation_skipsSourceValidation() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));
        when(locationRepository.findById(20L)).thenReturn(Optional.of(new Location()));
        when(userRepository.findById(30L)).thenReturn(Optional.of(new User()));
        when(movementRepository.save(any(PhysicalMovement.class))).thenAnswer(inv -> inv.getArgument(0));
        PhysicalMovementCreateDTO dto = dto();
        dto.setIdLocationFrom(null);

        PhysicalMovementResponseDTO response = movementService.create(dto);

        assertNull(response.getIdLocationFrom());
        verify(locationRepository, never()).findById(10L);
    }

    @Test
    void create_destinationLocationNotFound_throws() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(new Location()));
        when(locationRepository.findById(20L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> movementService.create(dto()));

        assertEquals("Destination location not found", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void create_userNotFound_throws() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(new Location()));
        when(locationRepository.findById(20L)).thenReturn(Optional.of(new Location()));
        when(userRepository.findById(30L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> movementService.create(dto()));

        assertEquals("User not found", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void findAll_mapsToDtos() {
        when(movementRepository.findAll()).thenReturn(List.of(movement(LocalDateTime.of(2026, 9, 22, 10, 0))));

        List<PhysicalMovementResponseDTO> response = movementService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdProduct());
        assertEquals(30L, response.get(0).getIdUser());
    }

    @Test
    void findByIdProduct_returnsList() {
        when(movementRepository.findByIdProductOrderByDateTimeDesc(1L))
                .thenReturn(List.of(movement(LocalDateTime.of(2026, 9, 22, 10, 0))));

        List<PhysicalMovementResponseDTO> response = movementService.findByIdProduct(1L);

        assertEquals(1, response.size());
        assertEquals(20L, response.get(0).getIdLocationTo());
    }

    @Test
    void findByIdUser_returnsList() {
        when(movementRepository.findByIdUserOrderByDateTimeDesc(30L))
                .thenReturn(List.of(movement(LocalDateTime.of(2026, 9, 22, 10, 0))));

        List<PhysicalMovementResponseDTO> response = movementService.findByIdUser(30L);

        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).getIdLocationFrom());
    }

    @Test
    void findByDateTimeRange_returnsList() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 30, 23, 59);
        when(movementRepository.findByDateTimeBetweenOrderByIdProductAscDateTimeAsc(from, to))
                .thenReturn(List.of(movement(to)));

        List<PhysicalMovementResponseDTO> response = movementService.findByDateTimeRange(from, to);

        assertEquals(1, response.size());
    }

    @Test
    void findByDateTimeRange_missingBoundary_throws() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> movementService.findByDateTimeRange(from, null));

        assertEquals("Both 'from' and 'to' are required for date range filtering", ex.getMessage());
    }
}