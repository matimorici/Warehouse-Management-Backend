package big_three.wms.service;


import big_three.wms.model.Location;
import big_three.wms.dto.LocationCreateDTO;
import big_three.wms.dto.LocationResponseDTO;
import big_three.wms.repository.LocationRepository;
import big_three.wms.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final CurrentUserProvider currentUserProvider;

    public LocationService(LocationRepository locationRepository, CurrentUserProvider currentUserProvider) {
        this.locationRepository = locationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public LocationResponseDTO create(LocationCreateDTO dto) {
        Location l = new Location();
        l.setName(dto.getName());
        Location saved = locationRepository.save(l);
        return convertToResponseDTO(saved);
    }

    public List<LocationResponseDTO> findAll() {
        return locationRepository.findByDeletedDateTimeIsNull()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    public LocationResponseDTO findById(Long id) {
        Location l = locationRepository.findByIdAndDeletedDateTimeIsNull(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        return convertToResponseDTO(l);
    }
    public LocationResponseDTO update(Long id, LocationCreateDTO dto) {
        Location l = locationRepository.findByIdAndDeletedDateTimeIsNull(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        l.setName(dto.getName());
        Location updated = locationRepository.save(l);
        return convertToResponseDTO(updated);
    }
    @Transactional
    public void deleteById(Long id) {
        Location l = locationRepository.findByIdAndDeletedDateTimeIsNull(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada para eliminar"));

        if (locationRepository.existsActiveProductCurrentlyInLocation(id)) {
            throw new IllegalArgumentException("No se puede dar de baja una ubicación que contiene productos");
        }

        l.setDeletedDateTime(LocalDateTime.now());
        l.setIdDeletedByUser(currentUserProvider.requireCurrentUserId());
        locationRepository.save(l);
    }

    private LocationResponseDTO convertToResponseDTO(Location location) {
        LocationResponseDTO response = new LocationResponseDTO();
        response.setId(location.getId());
        response.setName(location.getName());
        return response;
    }
}
