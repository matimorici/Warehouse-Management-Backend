package big_three.wms.service;

import big_three.wms.dto.PickOrderCreateDTO;
import big_three.wms.dto.PickOrderLineCreateDTO;
import big_three.wms.dto.PickOrderLineResponseDTO;
import big_three.wms.dto.PickOrderResponseDTO;
import big_three.wms.model.PickOrder;
import big_three.wms.model.PickOrderLine;
import big_three.wms.repository.PickOrderLineRepository;
import big_three.wms.repository.PickOrderRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.UserRepository;
import big_three.wms.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PickOrderService {

    private final PickOrderRepository pickOrderRepository;
    private final PickOrderLineRepository pickOrderLineRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final CurrentUserProvider currentUserProvider;

    public PickOrderService(PickOrderRepository pickOrderRepository,
            PickOrderLineRepository pickOrderLineRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductService productService,
            CurrentUserProvider currentUserProvider) {
        this.pickOrderRepository = pickOrderRepository;
        this.pickOrderLineRepository = pickOrderLineRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public PickOrderResponseDTO create(PickOrderCreateDTO dto) {
        if (!userRepository.existsById(dto.getIdUsuario())) {
            throw new RuntimeException("Usuario no encontrado");
        }
        for (PickOrderLineCreateDTO lineaRetiro : dto.getLineasRetiro()) {
            if (!productRepository.existsById(lineaRetiro.getIdProducto())) {
                throw new RuntimeException("Producto no encontrado: " + lineaRetiro.getIdProducto());
            }
        }

        PickOrder order = new PickOrder();
        order.setIdUsuario(dto.getIdUsuario());
        order.setFechaHora(LocalDateTime.now());
        PickOrder savedOrder = pickOrderRepository.save(order);

        for (PickOrderLineCreateDTO lineaRetiro : dto.getLineasRetiro()) {
            PickOrderLine line = new PickOrderLine();
            line.setIdOrdenRetiro(savedOrder.getIdOrdenRetiro());
            line.setIdProducto(lineaRetiro.getIdProducto());
            line.setCantidad(lineaRetiro.getCantidad());
            pickOrderLineRepository.save(line);
        }

        for (PickOrderLineCreateDTO lineaRetiro : dto.getLineasRetiro()) {
            productService.ajustarStock(lineaRetiro.getIdProducto(),
                    -lineaRetiro.getCantidad(), lineaRetiro.getCantidad());
        }

        return buildResponse(savedOrder);
    }

    public List<PickOrderResponseDTO> findAllSummaries() {
        return pickOrderRepository.findByFechaBajaIsNull()
                .stream()
                .map(this::buildSummary)
                .collect(Collectors.toList());
    }

    public PickOrderResponseDTO findById(Long id) {
        return buildResponse(findActiveOrThrow(id));
    }

    @Transactional
    public PickOrderResponseDTO update(Long id, PickOrderCreateDTO dto) {
        PickOrder order = findActiveOrThrow(id);

        if (!userRepository.existsById(dto.getIdUsuario())) {
            throw new RuntimeException("Usuario no encontrado");
        }
        for (PickOrderLineCreateDTO lineaRetiro : dto.getLineasRetiro()) {
            if (!productRepository.existsById(lineaRetiro.getIdProducto())) {
                throw new RuntimeException("Producto no encontrado: " + lineaRetiro.getIdProducto());
            }
        }

        order.setIdUsuario(dto.getIdUsuario());
        pickOrderRepository.save(order);

        List<PickOrderLine> oldLines = pickOrderLineRepository.findByIdOrdenRetiro(id);
        for (PickOrderLine oldLine : oldLines) {
            productService.ajustarStock(oldLine.getIdProducto(),
                    oldLine.getCantidad(), -oldLine.getCantidad());
        }

        pickOrderLineRepository.deleteAll(oldLines);

        for (PickOrderLineCreateDTO lineaRetiro : dto.getLineasRetiro()) {
            PickOrderLine line = new PickOrderLine();
            line.setIdOrdenRetiro(id);
            line.setIdProducto(lineaRetiro.getIdProducto());
            line.setCantidad(lineaRetiro.getCantidad());
            pickOrderLineRepository.save(line);
        }

        for (PickOrderLineCreateDTO lineaRetiro : dto.getLineasRetiro()) {
            productService.ajustarStock(lineaRetiro.getIdProducto(),
                    -lineaRetiro.getCantidad(), lineaRetiro.getCantidad());
        }

        return buildResponse(order);
    }

    @Transactional
    public void deleteById(Long id) {
        PickOrder order = findActiveOrThrow(id);
        List<PickOrderLine> lines = pickOrderLineRepository.findByIdOrdenRetiro(id);
        for (PickOrderLine line : lines) {
            productService.ajustarStock(line.getIdProducto(),
                    line.getCantidad(), -line.getCantidad());
        }
        order.setFechaBaja(LocalDateTime.now());
        order.setIdUsuarioBaja(currentUserProvider.requireCurrentUserId());
        pickOrderRepository.save(order);
    }

    private PickOrder findActiveOrThrow(Long id) {
        PickOrder order = pickOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de retiro no encontrada"));
        if (order.getFechaBaja() != null) {
            throw new RuntimeException("Orden de retiro no encontrada");
        }
        return order;
    }

    private PickOrderResponseDTO buildResponse(PickOrder order) {
        List<PickOrderLine> lines = pickOrderLineRepository.findByIdOrdenRetiro(order.getIdOrdenRetiro());
        List<PickOrderLineResponseDTO> lineasRetiro = lines.stream()
                .map(l -> new PickOrderLineResponseDTO(l.getIdProducto(), l.getCantidad()))
                .collect(Collectors.toList());
        return new PickOrderResponseDTO(
                order.getIdOrdenRetiro(),
                order.getFechaHora(),
                order.getIdUsuario(),
                lineasRetiro);
    }

    private PickOrderResponseDTO buildSummary(PickOrder order) {
        return new PickOrderResponseDTO(
                order.getIdOrdenRetiro(),
                order.getFechaHora(),
                order.getIdUsuario(),
                null);
    }
}
