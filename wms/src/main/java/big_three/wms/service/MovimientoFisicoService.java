package big_three.wms.service;

import big_three.wms.dto.MovimientoFisicoCreateDTO;
import big_three.wms.dto.MovimientoFisicoResponseDTO;
import big_three.wms.model.MovimientoFisico;
import big_three.wms.model.MovimientoFisico.MovimientoFisicoId;
import big_three.wms.repository.MovimientoFisicoRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.UbicacionRepository;
import big_three.wms.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovimientoFisicoService {

    private final MovimientoFisicoRepository movimientoFisicoRepository;
    private final ProductRepository productRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UserRepository userRepository;

    public MovimientoFisicoService(MovimientoFisicoRepository movimientoFisicoRepository,
                                   ProductRepository productRepository,
                                   UbicacionRepository ubicacionRepository,
                                   UserRepository userRepository) {
        this.movimientoFisicoRepository = movimientoFisicoRepository;
        this.productRepository = productRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MovimientoFisicoResponseDTO create(MovimientoFisicoCreateDTO dto) {
        if (!productRepository.existsById(dto.getIdProducto())) {
            throw new RuntimeException("Producto no encontrado");
        }
        if (!ubicacionRepository.existsById(dto.getIdUbicacionHasta())) {
            throw new RuntimeException("Ubicación de destino no encontrada");
        }
        if (dto.getIdUbicacionDesde() != null && !ubicacionRepository.existsById(dto.getIdUbicacionDesde())) {
            throw new RuntimeException("Ubicación de origen no encontrada");
        }
        if (!userRepository.existsById(dto.getIdUsuario())) {
            throw new RuntimeException("Usuario no encontrado");
        }

        MovimientoFisico m = new MovimientoFisico();
        m.setIdProducto(dto.getIdProducto());
        m.setFechaHora(LocalDateTime.now());
        m.setIdUbicacionDesde(dto.getIdUbicacionDesde());
        m.setIdUbicacionHasta(dto.getIdUbicacionHasta());
        m.setIdUsuario(dto.getIdUsuario());
        MovimientoFisico saved = movimientoFisicoRepository.save(m);
        return convertToResponseDTO(saved);
    }

    public List<MovimientoFisicoResponseDTO> findAll() {
        return movimientoFisicoRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<MovimientoFisicoResponseDTO> findByProducto(Long idProducto) {
        return movimientoFisicoRepository.findByIdProducto(idProducto)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public MovimientoFisicoResponseDTO findByIdProductoAndFechaHora(Long idProducto, LocalDateTime fechaHora) {
        MovimientoFisico m = movimientoFisicoRepository
                .findById(new MovimientoFisicoId(idProducto, fechaHora))
                .orElseThrow(() -> new RuntimeException("Movimiento físico no encontrado"));
        return convertToResponseDTO(m);
    }

    private MovimientoFisicoResponseDTO convertToResponseDTO(MovimientoFisico m) {
        MovimientoFisicoResponseDTO response = new MovimientoFisicoResponseDTO();
        response.setIdProducto(m.getIdProducto());
        response.setFechaHora(m.getFechaHora());
        response.setIdUbicacionDesde(m.getIdUbicacionDesde());
        response.setIdUbicacionHasta(m.getIdUbicacionHasta());
        response.setIdUsuario(m.getIdUsuario());
        return response;
    }
}
