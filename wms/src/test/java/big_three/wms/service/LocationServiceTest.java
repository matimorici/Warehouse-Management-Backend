package big_three.wms.service;

import big_three.wms.dto.LocationCreateDTO;
import big_three.wms.dto.LocationResponseDTO;
import big_three.wms.model.Location;
import big_three.wms.repository.LocationRepository;
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
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    private LocationCreateDTO dto(String name) {
        LocationCreateDTO dto = new LocationCreateDTO();
        dto.setName(name);
        return dto;
    }

    private Location location(Long id, String name) {
        Location location = new Location();
        location.setId(id);
        location.setName(name);
        return location;
    }

    @Test
    void create_success_returnsDto() {
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> {
            Location saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        LocationResponseDTO response = locationService.create(dto("Galpón Oeste"));

        assertEquals(1L, response.getId());
        assertEquals("Galpón Oeste", response.getName());
    }

    @Test
    void findAll_mapsToDtos() {
        when(locationRepository.findAll()).thenReturn(List.of(location(1L, "Galpón Oeste")));

        List<LocationResponseDTO> response = locationService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals("Galpón Oeste", response.get(0).getName());
    }

    @Test
    void findById_notFound_throws() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> locationService.findById(99L));
    }
    @Test
    void findById_success_returnsDto() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location(1L, "Galpón Oeste")));

        LocationResponseDTO response = locationService.findById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Galpón Oeste", response.getName());
    }
    @Test
    void update_success_updatesFields() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location(1L, "Galpón Oeste")));
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationResponseDTO response = locationService.update(1L, dto("Galpón Este"));

        assertEquals("Galpón Este", response.getName());
    }
    @Test
    void update_notFound_throws() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> locationService.update(99L, dto("Galpón Este")));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void deleteById_notFound_throws() {
        when(locationRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> locationService.deleteById(99L));
        verify(locationRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_existing_deletes() {
        when(locationRepository.existsById(1L)).thenReturn(true);

        locationService.deleteById(1L);

        verify(locationRepository).deleteById(1L);
    }
}
