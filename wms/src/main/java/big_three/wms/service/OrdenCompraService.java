package big_three.wms.service;

import big_three.wms.dto.LineaCompraCreateDTO;
import big_three.wms.dto.LineaCompraResponseDTO;
import big_three.wms.dto.OrdenCompraCreateDTO;
import big_three.wms.dto.OrdenCompraResponseDTO;
import big_three.wms.model.LineaCompra;
import big_three.wms.model.OrdenCompra;
import big_three.wms.model.OrdenCompra.EstadoOrdenCompra;
import big_three.wms.repository.LineaCompraRepository;
import big_three.wms.repository.OrdenCompraRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final LineaCompraRepository lineaCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public OrdenCompraService(OrdenCompraRepository ordenCompraRepository,
                              LineaCompraRepository lineaCompraRepository,
                              ProveedorRepository proveedorRepository,
                              ProductRepository productRepository,
                              ProductService productService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.lineaCompraRepository = lineaCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Transactional
    public OrdenCompraResponseDTO create(OrdenCompraCreateDTO dto) {
        if (!proveedorRepository.existsById(dto.getIdProveedor())) {
            throw new RuntimeException("Proveedor no encontrado");
        }
        for (LineaCompraCreateDTO linea : dto.getLineasCompra()) {
            if (!productRepository.existsById(linea.getIdProducto())) {
                throw new RuntimeException("Producto no encontrado: " + linea.getIdProducto());
            }
        }

        OrdenCompra orden = new OrdenCompra();
        orden.setIdProveedor(dto.getIdProveedor());
        orden.setFechaHora(LocalDateTime.now());
        orden.setEstado(EstadoOrdenCompra.PENDIENTE);
        OrdenCompra saved = ordenCompraRepository.save(orden);

        for (LineaCompraCreateDTO linea : dto.getLineasCompra()) {
            LineaCompra line = new LineaCompra();
            line.setIdOrdenCompra(saved.getIdOrdenCompra());
            line.setIdProducto(linea.getIdProducto());
            line.setCantidad(linea.getCantidad());
            lineaCompraRepository.save(line);
        }

        return buildResponse(saved);
    }

    public List<OrdenCompraResponseDTO> findAllSummaries() {
        return ordenCompraRepository.findAll()
                .stream()
                .map(this::buildSummary)
                .collect(Collectors.toList());
    }

    public OrdenCompraResponseDTO findById(Long id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada"));
        return buildResponse(orden);
    }

    @Transactional
    public OrdenCompraResponseDTO update(Long id, OrdenCompraCreateDTO dto, String estado) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada"));

        if (orden.getEstado() == EstadoOrdenCompra.RECIBIDA) {
            throw new IllegalArgumentException("No se puede modificar una orden de compra recibida");
        }
        if (!proveedorRepository.existsById(dto.getIdProveedor())) {
            throw new RuntimeException("Proveedor no encontrado");
        }
        for (LineaCompraCreateDTO linea : dto.getLineasCompra()) {
            if (!productRepository.existsById(linea.getIdProducto())) {
                throw new RuntimeException("Producto no encontrado: " + linea.getIdProducto());
            }
        }

        EstadoOrdenCompra nuevoEstado = EstadoOrdenCompra.PENDIENTE;
        if (estado != null && !estado.isBlank()) {
            try {
                nuevoEstado = EstadoOrdenCompra.valueOf(estado);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado inválido: debe ser PENDIENTE, RECIBIDA o CANCELADA");
            }
        }

        orden.setIdProveedor(dto.getIdProveedor());
        orden.setEstado(nuevoEstado);
        ordenCompraRepository.save(orden);

        List<LineaCompra> oldLines = lineaCompraRepository.findByIdOrdenCompra(id);
        lineaCompraRepository.deleteAll(oldLines);

        for (LineaCompraCreateDTO linea : dto.getLineasCompra()) {
            LineaCompra line = new LineaCompra();
            line.setIdOrdenCompra(id);
            line.setIdProducto(linea.getIdProducto());
            line.setCantidad(linea.getCantidad());
            lineaCompraRepository.save(line);
        }

        if (nuevoEstado == EstadoOrdenCompra.RECIBIDA) {
            for (LineaCompraCreateDTO linea : dto.getLineasCompra()) {
                productService.ajustarStock(linea.getIdProducto(), linea.getCantidad(), 0);
            }
        }

        return buildResponse(orden);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!ordenCompraRepository.existsById(id)) {
            throw new RuntimeException("Orden de compra no encontrada");
        }
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada"));
        if (orden.getEstado() == EstadoOrdenCompra.RECIBIDA) {
            throw new IllegalArgumentException("No se puede eliminar una orden de compra recibida");
        }
        List<LineaCompra> lines = lineaCompraRepository.findByIdOrdenCompra(id);
        lineaCompraRepository.deleteAll(lines);
        ordenCompraRepository.deleteById(id);
    }

    private OrdenCompraResponseDTO buildResponse(OrdenCompra orden) {
        List<LineaCompraResponseDTO> lineas = lineaCompraRepository.findByIdOrdenCompra(orden.getIdOrdenCompra())
                .stream()
                .map(l -> new LineaCompraResponseDTO(l.getIdProducto(), l.getCantidad()))
                .collect(Collectors.toList());
        return new OrdenCompraResponseDTO(
                orden.getIdOrdenCompra(),
                orden.getFechaHora(),
                orden.getIdProveedor(),
                orden.getEstado().name(),
                lineas);
    }

    private OrdenCompraResponseDTO buildSummary(OrdenCompra orden) {
        return new OrdenCompraResponseDTO(
                orden.getIdOrdenCompra(),
                orden.getFechaHora(),
                orden.getIdProveedor(),
                orden.getEstado().name(),
                null);
    }
}
