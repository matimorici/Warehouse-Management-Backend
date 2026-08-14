package big_three.wms.controller;

import big_three.wms.dto.OrdenCompraCreateDTO;
import big_three.wms.dto.OrdenCompraResponseDTO;
import big_three.wms.service.OrdenCompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ordenes-compra")
@CrossOrigin(origins = "http://localhost:4200")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    @PostMapping
    public ResponseEntity<OrdenCompraResponseDTO> create(@Valid @RequestBody OrdenCompraCreateDTO dto) {
        OrdenCompraResponseDTO response = ordenCompraService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrdenCompraResponseDTO>> list() {
        return ResponseEntity.ok(ordenCompraService.findAllSummaries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenCompraResponseDTO> update(@PathVariable Long id,
            @RequestParam(required = false) String estado,
            @Valid @RequestBody OrdenCompraCreateDTO dto) {
        return ResponseEntity.ok(ordenCompraService.update(id, dto, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ordenCompraService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
