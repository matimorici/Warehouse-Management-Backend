package big_three.wms.controller;

import big_three.wms.dto.ValoracionProveedorCreateDTO;
import big_three.wms.dto.ValoracionProveedorResponseDTO;
import big_three.wms.service.ValoracionProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/valoraciones-proveedor")
@CrossOrigin(origins = "http://localhost:4200")
public class ValoracionProveedorController {

    private final ValoracionProveedorService valoracionProveedorService;

    public ValoracionProveedorController(ValoracionProveedorService valoracionProveedorService) {
        this.valoracionProveedorService = valoracionProveedorService;
    }

    @PostMapping
    public ResponseEntity<ValoracionProveedorResponseDTO> create(@Valid @RequestBody ValoracionProveedorCreateDTO dto) {
        ValoracionProveedorResponseDTO response = valoracionProveedorService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ValoracionProveedorResponseDTO>> list() {
        return ResponseEntity.ok(valoracionProveedorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ValoracionProveedorResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(valoracionProveedorService.findById(id));
    }

    @GetMapping("/proveedor/{idProveedor}")
    public ResponseEntity<List<ValoracionProveedorResponseDTO>> listByProveedor(@PathVariable Long idProveedor) {
        return ResponseEntity.ok(valoracionProveedorService.findByProveedor(idProveedor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ValoracionProveedorResponseDTO> update(@PathVariable Long id,
            @Valid @RequestBody ValoracionProveedorCreateDTO dto) {
        return ResponseEntity.ok(valoracionProveedorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        valoracionProveedorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
