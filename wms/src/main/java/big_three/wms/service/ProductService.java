package big_three.wms.service;

import big_three.wms.model.Product;
import big_three.wms.model.Product.OrigenCodigoBarras;
import big_three.wms.dto.ProductCreateDTO;
import big_three.wms.dto.ProductResponseDTO;
import big_three.wms.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponseDTO create(ProductCreateDTO dto) {
        String codigoBarras;
        OrigenCodigoBarras origen;

        if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().isBlank()) {
            if (productRepository.existsByCodigoBarras(dto.getCodigoBarras())) {
                throw new IllegalArgumentException("Ya existe un producto con ese código de barras: " + dto.getCodigoBarras());
            }
            codigoBarras = dto.getCodigoBarras();
            origen = OrigenCodigoBarras.FABRICANTE;
        } else {
            // No trae código -> generamos uno interno
            codigoBarras = generarCodigoInterno();
            origen = OrigenCodigoBarras.INTERNO;
        }
        Product p = new Product();
        p.setNombreProducto(dto.getNombreProducto());
        p.setDescripcionProducto(dto.getDescripcionProducto());
        p.setCodigoBarras(codigoBarras);
        p.setIdProveedor(dto.getIdProveedor());
        p.setOrigenCodigoBarras(origen);
        Product savedProduct = productRepository.save(p);
        return convertToResponseDTO(savedProduct);
    }
    private ProductResponseDTO convertToResponseDTO(Product product) {
        ProductResponseDTO response = new ProductResponseDTO();
        response.setIdProducto(product.getIdProducto());
        response.setNombreProducto(product.getNombreProducto());
        response.setDescripcionProducto(product.getDescripcionProducto());
        response.setCodigoBarras(product.getCodigoBarras());
        response.setIdProveedor(product.getIdProveedor());
        response.setOrigenCodigoBarras(product.getOrigenCodigoBarras().name());
        return response;
    }
    private String generarCodigoInterno() {
        Long siguiente = productRepository.obtenerSiguienteSecuencia(); // SELECT nextval('codigo_interno_seq')
        return String.format("INT-%06d", siguiente);
    }
    // Retorna una lista de DTOs mapeados
    public List<ProductResponseDTO> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    // Retorna un DTO de respuesta o lanza error
    public ProductResponseDTO findById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return convertToResponseDTO(p);
    }
    public void deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado para eliminar");
        }
        productRepository.deleteById(id);
    }
}