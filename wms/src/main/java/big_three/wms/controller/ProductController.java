package big_three.wms.controller;

import big_three.wms.dto.ProductCreateDTO;
import big_three.wms.dto.ProductResponseDTO;
import big_three.wms.dto.StockResponseDTO;
import big_three.wms.dto.StockUpdateDTO;
import big_three.wms.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(@Valid @RequestBody ProductCreateDTO dto) {
        // El service ahora devuelve directamente el DTO limpio sin password
        ProductResponseDTO response = productService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> list() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(@PathVariable Long id,
            @Valid @RequestBody ProductCreateDTO dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<StockResponseDTO> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findStockByIdProducto(id));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<StockResponseDTO> updateStock(@PathVariable Long id,
            @Valid @RequestBody StockUpdateDTO dto) {
        return ResponseEntity.ok(productService.updateStock(id, dto));
    }
}