package big_three.wms.controller;

import big_three.wms.dto.PurchaseOrderCreateDTO;
import big_three.wms.dto.PurchaseOrderResponseDTO;
import big_three.wms.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
@RestController
@RequestMapping("/api/ordenes-compra")
@CrossOrigin(origins = "http://localhost:4200")

public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }


    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDTO> create(@Valid @RequestBody PurchaseOrderCreateDTO dto) {
        PurchaseOrderResponseDTO response = purchaseOrderService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponseDTO>> list() {
        return ResponseEntity.ok(purchaseOrderService.findAllSummaries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDTO> update(@PathVariable Long id,
                                                           @Valid @RequestBody PurchaseOrderCreateDTO dto) {
        return ResponseEntity.ok(purchaseOrderService.update(id, dto));
    }

    @PutMapping("/{id}/recibir")
    public ResponseEntity<PurchaseOrderResponseDTO> receive(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.receive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaseOrderService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
