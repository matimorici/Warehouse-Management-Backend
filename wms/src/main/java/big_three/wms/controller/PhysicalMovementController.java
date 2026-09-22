package big_three.wms.controller;

import big_three.wms.dto.PhysicalMovementCreateDTO;
import big_three.wms.dto.PhysicalMovementResponseDTO;
import big_three.wms.service.PhysicalMovementService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos-fisicos")
@CrossOrigin(origins = "http://localhost:4200")
public class PhysicalMovementController {
    private final PhysicalMovementService movementService;

    public PhysicalMovementController(PhysicalMovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping
    public ResponseEntity<PhysicalMovementResponseDTO> create(@Valid @RequestBody PhysicalMovementCreateDTO createDTO) {
        PhysicalMovementResponseDTO response = movementService.create(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PhysicalMovementResponseDTO>> list(
            @RequestParam(required = false) Long idProduct,
            @RequestParam(required = false) Long idUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (idProduct != null) {
            return ResponseEntity.ok(movementService.findByIdProduct(idProduct));
        }
        if (idUser != null) {
            return ResponseEntity.ok(movementService.findByIdUser(idUser));
        }
        if (from != null || to != null) {
            return ResponseEntity.ok(movementService.findByDateTimeRange(from, to));
        }
        return ResponseEntity.ok(movementService.findAll());
    }
}