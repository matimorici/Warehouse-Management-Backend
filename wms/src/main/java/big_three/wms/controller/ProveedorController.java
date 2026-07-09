package big_three.wms.controller;

import big_three.wms.dto.ProveedorCreateDTO;
import big_three.wms.dto.ProveedorResponseDTO;
import big_three.wms.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "http://localhost:4200")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @PostMapping
    public ResponseEntity<ProveedorResponseDTO> create(@Valid @RequestBody ProveedorCreateDTO dto) {
        ProveedorResponseDTO response = proveedorService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProveedorResponseDTO>> list() {
        return ResponseEntity.ok(proveedorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> update(@PathVariable Long id,
            @Valid @RequestBody ProveedorCreateDTO dto) {
        return ResponseEntity.ok(proveedorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        proveedorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
