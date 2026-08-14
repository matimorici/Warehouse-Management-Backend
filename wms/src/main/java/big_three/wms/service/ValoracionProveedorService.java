package big_three.wms.service;

import big_three.wms.model.Proveedor;
import big_three.wms.model.ValoracionProveedor;
import big_three.wms.dto.ValoracionProveedorCreateDTO;
import big_three.wms.dto.ValoracionProveedorResponseDTO;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.ValoracionProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ValoracionProveedorService {

    private final ValoracionProveedorRepository valoracionProveedorRepository;
    private final ProveedorRepository proveedorRepository;

    public ValoracionProveedorService(ValoracionProveedorRepository valoracionProveedorRepository,
                                      ProveedorRepository proveedorRepository) {
        this.valoracionProveedorRepository = valoracionProveedorRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional
    public ValoracionProveedorResponseDTO create(ValoracionProveedorCreateDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        ValoracionProveedor v = new ValoracionProveedor();
        v.setProveedor(proveedor);
        v.setFechaHora(LocalDateTime.now());
        v.setTiempoEntrega(dto.getTiempoEntrega());
        v.setFormaEntrega(dto.getFormaEntrega());
        v.setRelacionPrecioCalidad(dto.getRelacionPrecioCalidad());
        ValoracionProveedor saved = valoracionProveedorRepository.save(v);
        return convertToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ValoracionProveedorResponseDTO> findAll() {
        return valoracionProveedorRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ValoracionProveedorResponseDTO findById(Long id) {
        ValoracionProveedor v = valoracionProveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        return convertToResponseDTO(v);
    }

    @Transactional(readOnly = true)
    public List<ValoracionProveedorResponseDTO> findByProveedor(Long idProveedor) {
        if (!proveedorRepository.existsById(idProveedor)) {
            throw new RuntimeException("Proveedor no encontrado");
        }
        return valoracionProveedorRepository.findByIdProveedor(idProveedor)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ValoracionProveedorResponseDTO update(Long id, ValoracionProveedorCreateDTO dto) {
        ValoracionProveedor v = valoracionProveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        v.setProveedor(proveedor);
        v.setTiempoEntrega(dto.getTiempoEntrega());
        v.setFormaEntrega(dto.getFormaEntrega());
        v.setRelacionPrecioCalidad(dto.getRelacionPrecioCalidad());
        ValoracionProveedor updated = valoracionProveedorRepository.save(v);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!valoracionProveedorRepository.existsById(id)) {
            throw new RuntimeException("Valoración no encontrada para eliminar");
        }
        valoracionProveedorRepository.deleteById(id);
    }

    private ValoracionProveedorResponseDTO convertToResponseDTO(ValoracionProveedor v) {
        ValoracionProveedorResponseDTO response = new ValoracionProveedorResponseDTO();
        response.setIdValoracion(v.getIdValoracion());
        response.setIdProveedor(v.getProveedor().getIdProveedor());
        response.setFechaHora(v.getFechaHora());
        response.setTiempoEntrega(v.getTiempoEntrega());
        response.setFormaEntrega(v.getFormaEntrega());
        response.setRelacionPrecioCalidad(v.getRelacionPrecioCalidad());
        return response;
    }
}
