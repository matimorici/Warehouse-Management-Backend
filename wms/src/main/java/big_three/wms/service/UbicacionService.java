package big_three.wms.service;

import big_three.wms.model.Ubicacion;
import big_three.wms.dto.UbicacionCreateDTO;
import big_three.wms.dto.UbicacionResponseDTO;
import big_three.wms.repository.UbicacionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UbicacionService {

    private final UbicacionRepository ubicacionRepository;

    public UbicacionService(UbicacionRepository ubicacionRepository) {
        this.ubicacionRepository = ubicacionRepository;
    }

    public UbicacionResponseDTO create(UbicacionCreateDTO dto) {
        Ubicacion u = new Ubicacion();
        u.setNombreUbicacion(dto.getNombreUbicacion());
        Ubicacion saved = ubicacionRepository.save(u);
        return convertToResponseDTO(saved);
    }

    public List<UbicacionResponseDTO> findAll() {
        return ubicacionRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public UbicacionResponseDTO findById(Long id) {
        Ubicacion u = ubicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        return convertToResponseDTO(u);
    }

    public UbicacionResponseDTO update(Long id, UbicacionCreateDTO dto) {
        Ubicacion u = ubicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        u.setNombreUbicacion(dto.getNombreUbicacion());
        Ubicacion updated = ubicacionRepository.save(u);
        return convertToResponseDTO(updated);
    }

    public void deleteById(Long id) {
        if (!ubicacionRepository.existsById(id)) {
            throw new RuntimeException("Ubicación no encontrada para eliminar");
        }
        ubicacionRepository.deleteById(id);
    }

    private UbicacionResponseDTO convertToResponseDTO(Ubicacion ubicacion) {
        UbicacionResponseDTO response = new UbicacionResponseDTO();
        response.setIdUbicacion(ubicacion.getIdUbicacion());
        response.setNombreUbicacion(ubicacion.getNombreUbicacion());
        return response;
    }
}
