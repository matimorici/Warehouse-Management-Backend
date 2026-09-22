package big_three.wms.service;


import big_three.wms.model.Location;
import big_three.wms.dto.LocationCreateDTO;
import big_three.wms.dto.LocationResponseDTO;
import big_three.wms.repository.LocationRepository;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public LocationResponseDTO create(LocationCreateDTO dto) {
        Location l = new Location();
        l.setName(dto.getName());
        Location saved = locationRepository.save(l);
        return convertToResponseDTO(saved);
    }

    public List<LocationResponseDTO> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    public LocationResponseDTO findById(Long id) {
        Location l = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        return convertToResponseDTO(l);
    }
    public LocationResponseDTO update(Long id, LocationCreateDTO dto) {
        Location l = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        l.setName(dto.getName());
        Location updated = locationRepository.save(l);
        return convertToResponseDTO(updated);
    }
    public void deleteById(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new RuntimeException("Ubicación no encontrada para eliminar");
        }
        locationRepository.deleteById(id);
    }

    private LocationResponseDTO convertToResponseDTO(Location location) {
        LocationResponseDTO response = new LocationResponseDTO();
        response.setId(location.getId());
        response.setName(location.getName());
        return response;
    }
}
