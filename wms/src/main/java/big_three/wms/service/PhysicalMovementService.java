package big_three.wms.service;

import big_three.wms.dto.PhysicalMovementCreateDTO;
import big_three.wms.dto.PhysicalMovementResponseDTO;
import big_three.wms.model.PhysicalMovement;
import big_three.wms.repository.LocationRepository;
import big_three.wms.repository.PhysicalMovementRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhysicalMovementService {
    // Append-only service: only records movement events; it never updates or deletes them,
    // and it does not touch Stock.
    private final PhysicalMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public PhysicalMovementService(PhysicalMovementRepository movementRepository,
                                   ProductRepository productRepository,
                                   LocationRepository locationRepository,
                                   UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PhysicalMovementResponseDTO create(PhysicalMovementCreateDTO dto) {
        productRepository.findById(dto.getIdProduct())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (dto.getIdLocationFrom() != null) {
            Long idLocationFrom = dto.getIdLocationFrom();
            locationRepository.findById(idLocationFrom)
                    .orElseThrow(() -> new IllegalArgumentException("Ubicación de origen no encontrada"));
        }
        locationRepository.findById(dto.getIdLocationTo())
                .orElseThrow(() -> new IllegalArgumentException("Ubicación de destino no encontrada"));
        userRepository.findById(dto.getIdUser())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        PhysicalMovement movement = new PhysicalMovement();
        movement.setIdProduct(dto.getIdProduct());
        movement.setDateTime(LocalDateTime.now());
        movement.setIdLocationFrom(dto.getIdLocationFrom());
        movement.setIdLocationTo(dto.getIdLocationTo());
        movement.setIdUser(dto.getIdUser());

        PhysicalMovement saved = movementRepository.save(movement);
        return convertToResponseDTO(saved);
    }

    private PhysicalMovementResponseDTO convertToResponseDTO(PhysicalMovement movement) {
        PhysicalMovementResponseDTO responseDTO = new PhysicalMovementResponseDTO();
        responseDTO.setIdProduct(movement.getIdProduct());
        responseDTO.setDateTime(movement.getDateTime());
        responseDTO.setIdLocationFrom(movement.getIdLocationFrom());
        responseDTO.setIdLocationTo(movement.getIdLocationTo());
        responseDTO.setIdUser(movement.getIdUser());
        return responseDTO;
    }

    public List<PhysicalMovementResponseDTO> findAll() {
        return movementRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<PhysicalMovementResponseDTO> findByIdProduct(Long idProduct) {
        return movementRepository.findByIdProductOrderByDateTimeDesc(idProduct).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<PhysicalMovementResponseDTO> findByIdUser(Long idUser) {
        return movementRepository.findByIdUserOrderByDateTimeDesc(idUser).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<PhysicalMovementResponseDTO> findByDateTimeRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Se requieren tanto fecha de inicio como de fin");
        }
        return movementRepository.findByDateTimeBetweenOrderByIdProductAscDateTimeAsc(from, to).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
}