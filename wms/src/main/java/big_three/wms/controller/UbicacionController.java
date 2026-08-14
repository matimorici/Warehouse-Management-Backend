package big_three.wms.controller;

import big_three.wms.dto.UbicacionCreateDTO;
import big_three.wms.dto.UbicacionResponseDTO;
import big_three.wms.service.UbicacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
@CrossOrigin(origins = "http://localhost:4200")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @PostMapping
    public ResponseEntity<UbicacionResponseDTO> create(@Valid @RequestBody UbicacionCreateDTO dto) {
        UbicacionResponseDTO response = ubicacionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UbicacionResponseDTO>> list() {
        return ResponseEntity.ok(ubicacionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UbicacionResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(ubicacionService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UbicacionResponseDTO> update(@PathVariable Long id,
            @Valid @RequestBody UbicacionCreateDTO dto) {
        return ResponseEntity.ok(ubicacionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ubicacionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
