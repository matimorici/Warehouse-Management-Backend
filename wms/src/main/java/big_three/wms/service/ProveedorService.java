package big_three.wms.service;

import big_three.wms.model.Proveedor;
import big_three.wms.dto.ProveedorCreateDTO;
import big_three.wms.dto.ProveedorResponseDTO;
import big_three.wms.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public ProveedorResponseDTO create(ProveedorCreateDTO dto) {
        if (proveedorRepository.existsByCuit(dto.getCuit())) {
            throw new IllegalArgumentException("Ya existe un proveedor con ese CUIT: " + dto.getCuit());
        }
        Proveedor p = new Proveedor();
        p.setCuit(dto.getCuit());
        p.setRazonSocial(dto.getRazonSocial());
        p.setTelefono(dto.getTelefono());
        p.setMail(dto.getMail());
        p.setDireccion(dto.getDireccion());
        Proveedor saved = proveedorRepository.save(p);
        return convertToResponseDTO(saved);
    }

    public List<ProveedorResponseDTO> findAll() {
        return proveedorRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public ProveedorResponseDTO findById(Long id) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        return convertToResponseDTO(p);
    }

    public ProveedorResponseDTO update(Long id, ProveedorCreateDTO dto) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        if (!p.getCuit().equals(dto.getCuit()) && proveedorRepository.existsByCuit(dto.getCuit())) {
            throw new IllegalArgumentException("Ya existe un proveedor con ese CUIT: " + dto.getCuit());
        }
        p.setCuit(dto.getCuit());
        p.setRazonSocial(dto.getRazonSocial());
        p.setTelefono(dto.getTelefono());
        p.setMail(dto.getMail());
        p.setDireccion(dto.getDireccion());
        Proveedor updated = proveedorRepository.save(p);
        return convertToResponseDTO(updated);
    }

    public void deleteById(Long id) {
        if (!proveedorRepository.existsById(id)) {
            throw new RuntimeException("Proveedor no encontrado para eliminar");
        }
        proveedorRepository.deleteById(id);
    }

    private ProveedorResponseDTO convertToResponseDTO(Proveedor proveedor) {
        ProveedorResponseDTO response = new ProveedorResponseDTO();
        response.setIdProveedor(proveedor.getIdProveedor());
        response.setCuit(proveedor.getCuit());
        response.setRazonSocial(proveedor.getRazonSocial());
        response.setTelefono(proveedor.getTelefono());
        response.setMail(proveedor.getMail());
        response.setDireccion(proveedor.getDireccion());
        return response;
    }
}
