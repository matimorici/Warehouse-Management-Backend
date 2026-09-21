package big_three.wms.controller;


import big_three.wms.dto.*;
import big_three.wms.service.SupplierRatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/valoraciones-proveedor")
@CrossOrigin(origins = "http://localhost:4200")
public class SupplierRatingController {
    private final SupplierRatingService ratingService;
    public SupplierRatingController(SupplierRatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<SupplierRatingResponseDTO> create(@Valid @RequestBody SupplierRatingCreateDTO createDTO) {
        SupplierRatingResponseDTO response = ratingService.create(createDTO);
        return  ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SupplierRatingResponseDTO>> list(
            @RequestParam(required = false) Long idSupplier) {
        if (idSupplier != null) {
            return ResponseEntity.ok(ratingService.findBySupplier(idSupplier));
        }
        return ResponseEntity.ok(ratingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierRatingResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(ratingService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierRatingResponseDTO> update(@PathVariable Long id, @Valid @RequestBody SupplierRatingCreateDTO dto) {
        return ResponseEntity.ok(ratingService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ratingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
