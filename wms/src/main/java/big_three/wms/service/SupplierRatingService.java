package big_three.wms.service;

import big_three.wms.dto.SupplierRatingCreateDTO;
import big_three.wms.dto.SupplierRatingResponseDTO;
import big_three.wms.model.Proveedor;
import big_three.wms.model.SupplierRating;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.SupplierRatingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierRatingService {
    private final SupplierRatingRepository ratingRepository;
    private final ProveedorRepository supplierRepository;

    public SupplierRatingService(SupplierRatingRepository ratingRepository, ProveedorRepository supplierRepository) {
        this.ratingRepository = ratingRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public SupplierRatingResponseDTO create (SupplierRatingCreateDTO dto){
        Proveedor supplier = supplierRepository.findById(dto.getIdSupplier())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        SupplierRating sr = new SupplierRating();
        sr.setIdSupplier(dto.getIdSupplier());
        sr.setDateTime(LocalDateTime.now());
        sr.setDeliveryMethod(dto.getDeliveryMethod());
        sr.setDeliveryTime(dto.getDeliveryTime());
        sr.setPriceQualityRatio(dto.getPriceQualityRatio());
        SupplierRating savedSR = ratingRepository.save(sr);

        return convertToResponseDTO(savedSR);
    }

    private SupplierRatingResponseDTO convertToResponseDTO(SupplierRating sr){
        SupplierRatingResponseDTO responseDTO = new SupplierRatingResponseDTO();
        responseDTO.setId(sr.getId());
        responseDTO.setIdSupplier(sr.getIdSupplier());
        responseDTO.setDeliveryMethod(sr.getDeliveryMethod());
        responseDTO.setDeliveryTime(sr.getDeliveryTime());
        responseDTO.setPriceQualityRatio(sr.getPriceQualityRatio());
        responseDTO.setDateTime(sr.getDateTime());
        return responseDTO;

    }

    public List<SupplierRatingResponseDTO> findAll(){
        return ratingRepository.findAll().stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    public SupplierRatingResponseDTO findById(Long id) {
        SupplierRating sr = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        return convertToResponseDTO(sr);
    }

    public List<SupplierRatingResponseDTO> findBySupplier(Long idSupplier) {
        if (!supplierRepository.existsById(idSupplier)) {
            throw new RuntimeException("Proveedor no encontrado");
        }
        return ratingRepository.findByIdSupplier(idSupplier)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public SupplierRatingResponseDTO update(Long id, SupplierRatingCreateDTO dto){
        SupplierRating sr = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        sr.setDeliveryMethod(dto.getDeliveryMethod());
        sr.setDeliveryTime(dto.getDeliveryTime());
        sr.setPriceQualityRatio(dto.getPriceQualityRatio());
        sr.setDateTime(LocalDateTime.now());
        ratingRepository.save(sr);
        return convertToResponseDTO(sr);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!ratingRepository.existsById(id)) {
            throw new RuntimeException("Valoración no encontrada para eliminar");
        }
        ratingRepository.deleteById(id);
    }

}
