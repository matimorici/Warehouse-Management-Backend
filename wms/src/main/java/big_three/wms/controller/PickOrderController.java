package big_three.wms.controller;

import big_three.wms.dto.PickOrderCreateDTO;
import big_three.wms.dto.PickOrderResponseDTO;
import big_three.wms.service.PickOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes-retiro")
@CrossOrigin(origins = "http://localhost:4200")
public class PickOrderController {

    private final PickOrderService pickOrderService;

    public PickOrderController(PickOrderService pickOrderService) {
        this.pickOrderService = pickOrderService;
    }

    @PostMapping
    public ResponseEntity<PickOrderResponseDTO> create(@Valid @RequestBody PickOrderCreateDTO dto) {
        PickOrderResponseDTO response = pickOrderService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PickOrderResponseDTO>> list() {
        return ResponseEntity.ok(pickOrderService.findAllSummaries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PickOrderResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(pickOrderService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PickOrderResponseDTO> update(@PathVariable Long id,
            @Valid @RequestBody PickOrderCreateDTO dto) {
        return ResponseEntity.ok(pickOrderService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pickOrderService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
