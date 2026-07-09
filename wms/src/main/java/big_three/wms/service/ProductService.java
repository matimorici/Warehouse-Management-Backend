package big_three.wms.service;

import big_three.wms.model.Product;
import big_three.wms.model.Product.OrigenCodigoBarras;
import big_three.wms.model.Proveedor;
import big_three.wms.model.Stock;
import big_three.wms.dto.ProductCreateDTO;
import big_three.wms.dto.ProductResponseDTO;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.StockRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProveedorRepository proveedorRepository;
    private final StockRepository stockRepository;

    public ProductService(ProductRepository productRepository,
                          ProveedorRepository proveedorRepository,
                          StockRepository stockRepository) {
        this.productRepository = productRepository;
        this.proveedorRepository = proveedorRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public ProductResponseDTO create(ProductCreateDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        String codigoBarras;
        OrigenCodigoBarras origen;

        if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().isBlank()) {
            if (productRepository.existsByCodigoBarras(dto.getCodigoBarras())) {
                throw new IllegalArgumentException("Ya existe un producto con ese código de barras: " + dto.getCodigoBarras());
            }
            codigoBarras = dto.getCodigoBarras();
            origen = OrigenCodigoBarras.FABRICANTE;
        } else {
            codigoBarras = generarCodigoInterno();
            origen = OrigenCodigoBarras.INTERNO;
        }
        Product p = new Product();
        p.setNombreProducto(dto.getNombreProducto());
        p.setDescripcionProducto(dto.getDescripcionProducto());
        p.setCodigoBarras(codigoBarras);
        p.setProveedor(proveedor);
        p.setOrigenCodigoBarras(origen);
        Product savedProduct = productRepository.save(p);

        Stock stock = new Stock();
        stock.setIdProducto(savedProduct.getIdProducto());
        stock.setFechaHora(LocalDateTime.now());
        stock.setCantidadDisponible(dto.getCantidadDisponible() != null ? dto.getCantidadDisponible() : 0);
        stock.setCantidadPendiente(dto.getCantidadPendiente() != null ? dto.getCantidadPendiente() : 0);
        stockRepository.save(stock);

        return convertToResponseDTO(savedProduct, stock);
    }

    @Transactional
    public ProductResponseDTO update(Long id, ProductCreateDTO dto) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().isBlank()) {
            if (!p.getCodigoBarras().equals(dto.getCodigoBarras()) &&
                    productRepository.existsByCodigoBarras(dto.getCodigoBarras())) {
                throw new IllegalArgumentException("Ya existe un producto con ese código de barras: " + dto.getCodigoBarras());
            }
            p.setCodigoBarras(dto.getCodigoBarras());
            p.setOrigenCodigoBarras(OrigenCodigoBarras.FABRICANTE);
        }
        p.setNombreProducto(dto.getNombreProducto());
        p.setDescripcionProducto(dto.getDescripcionProducto());
        p.setProveedor(proveedor);
        if (dto.getOrigenCodigoBarras() != null) {
            p.setOrigenCodigoBarras(OrigenCodigoBarras.valueOf(dto.getOrigenCodigoBarras()));
        }
        productRepository.save(p);

        Stock stock = stockRepository.findById(id).orElseGet(() -> {
            Stock s = new Stock();
            s.setIdProducto(id);
            s.setFechaHora(LocalDateTime.now());
            s.setCantidadDisponible(0);
            s.setCantidadPendiente(0);
            return s;
        });
        if (dto.getCantidadDisponible() != null) {
            stock.setCantidadDisponible(dto.getCantidadDisponible());
        }
        if (dto.getCantidadPendiente() != null) {
            stock.setCantidadPendiente(dto.getCantidadPendiente());
        }
        stock.setFechaHora(LocalDateTime.now());
        stockRepository.save(stock);

        return convertToResponseDTO(p, stock);
    }

    private ProductResponseDTO convertToResponseDTO(Product product, Stock stock) {
        ProductResponseDTO response = new ProductResponseDTO();
        response.setIdProducto(product.getIdProducto());
        response.setNombreProducto(product.getNombreProducto());
        response.setDescripcionProducto(product.getDescripcionProducto());
        response.setCodigoBarras(product.getCodigoBarras());
        response.setIdProveedor(product.getProveedor().getIdProveedor());
        response.setOrigenCodigoBarras(product.getOrigenCodigoBarras().name());
        if (stock != null) {
            response.setCantidadDisponible(stock.getCantidadDisponible());
            response.setCantidadPendiente(stock.getCantidadPendiente());
            response.setStockFechaHora(stock.getFechaHora());
        }
        return response;
    }

    private String generarCodigoInterno() {
        Long siguiente = productRepository.obtenerSiguienteSecuencia();
        return String.format("INT-%06d", siguiente);
    }

    public List<ProductResponseDTO> findAll() {
        return productRepository.findAll()
                .stream()
                .map(p -> {
                    Stock stock = stockRepository.findById(p.getIdProducto()).orElse(null);
                    return convertToResponseDTO(p, stock);
                })
                .collect(Collectors.toList());
    }

    public ProductResponseDTO findById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        Stock stock = stockRepository.findById(id).orElse(null);
        return convertToResponseDTO(p, stock);
    }

    public void deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado para eliminar");
        }
        productRepository.deleteById(id);
    }
}