package big_three.wms.service;

import big_three.wms.dto.SupplierRatingCreateDTO;
import big_three.wms.dto.SupplierRatingResponseDTO;
import big_three.wms.model.Proveedor;
import big_three.wms.model.SupplierRating;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.SupplierRatingRepository;
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
class SupplierRatingServiceTest {

    @Mock
    private SupplierRatingRepository ratingRepository;

    @Mock
    private ProveedorRepository supplierRepository;

    @InjectMocks
    private SupplierRatingService ratingService;

    private SupplierRatingCreateDTO dto() {
        SupplierRatingCreateDTO dto = new SupplierRatingCreateDTO();
        dto.setIdSupplier(1L);
        dto.setDeliveryTime(2);
        dto.setDeliveryMethod("Entrega a domicilio");
        dto.setPriceQualityRatio("Buena");
        return dto;
    }

    private SupplierRating rating(Long id) {
        SupplierRating rating = new SupplierRating();
        rating.setId(id);
        rating.setIdSupplier(1L);
        rating.setDateTime(LocalDateTime.of(2026, 9, 21, 10, 0));
        rating.setDeliveryTime(2);
        rating.setDeliveryMethod("Entrega a domicilio");
        rating.setPriceQualityRatio("Buena");
        return rating;
    }

    @Test
    void create_success_returnsDto() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(new Proveedor()));
        when(ratingRepository.save(any(SupplierRating.class))).thenAnswer(inv -> {
            SupplierRating saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SupplierRatingResponseDTO response = ratingService.create(dto());

        assertEquals(1L, response.getId());
        assertNotNull(response.getDateTime());
        assertEquals(2, response.getDeliveryTime());
        assertEquals("Entrega a domicilio", response.getDeliveryMethod());
        verify(ratingRepository).save(any(SupplierRating.class));
    }

    @Test
    void create_supplierNotFound_throws() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());
        SupplierRatingCreateDTO dto = dto();
        dto.setIdSupplier(99L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ratingService.create(dto));

        assertEquals("Proveedor no encontrado", ex.getMessage());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void findAll_mapsToDtos() {
        when(ratingRepository.findAll()).thenReturn(List.of(rating(1L)));

        List<SupplierRatingResponseDTO> response = ratingService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals("Buena", response.get(0).getPriceQualityRatio());
    }

    @Test
    void findById_notFound_throws() {
        when(ratingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> ratingService.findById(99L));
    }

    @Test
    void findBySupplier_supplierNotFound_throws() {
        when(supplierRepository.existsById(99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ratingService.findBySupplier(99L));

        assertEquals("Proveedor no encontrado", ex.getMessage());
        verify(ratingRepository, never()).findByIdSupplier(any());
    }

    @Test
    void findBySupplier_noRatings_returnsEmpty() {
        when(supplierRepository.existsById(1L)).thenReturn(true);
        when(ratingRepository.findByIdSupplier(1L)).thenReturn(List.of());

        List<SupplierRatingResponseDTO> response = ratingService.findBySupplier(1L);

        assertTrue(response.isEmpty());
    }

    @Test
    void findBySupplier_returnsList() {
        when(supplierRepository.existsById(1L)).thenReturn(true);
        when(ratingRepository.findByIdSupplier(1L)).thenReturn(List.of(rating(1L)));

        List<SupplierRatingResponseDTO> response = ratingService.findBySupplier(1L);

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdSupplier());
    }

    @Test
    void update_success_updatesFields() {
        SupplierRating existing = rating(1L);
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(SupplierRating.class))).thenAnswer(inv -> inv.getArgument(0));
        SupplierRatingCreateDTO dto = dto();
        dto.setDeliveryTime(5);
        dto.setDeliveryMethod("Punto de venta");

        SupplierRatingResponseDTO response = ratingService.update(1L, dto);

        assertEquals(5, response.getDeliveryTime());
        assertEquals("Punto de venta", response.getDeliveryMethod());
        assertEquals("Buena", response.getPriceQualityRatio());
        assertEquals(1L, existing.getIdSupplier());
    }

    @Test
    void update_notFound_throws() {
        when(ratingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> ratingService.update(99L, dto()));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(ratingRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> ratingService.deleteById(99L));
        verify(ratingRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_existing_deletes() {
        when(ratingRepository.existsById(1L)).thenReturn(true);

        ratingService.deleteById(1L);

        verify(ratingRepository).deleteById(1L);
    }
}