package big_three.wms.service;

import big_three.wms.dto.PurchaseOrderCreateDTO;
import big_three.wms.dto.PurchaseOrderLineCreateDTO;
import big_three.wms.dto.PurchaseOrderLineResponseDTO;
import big_three.wms.dto.PurchaseOrderResponseDTO;
import big_three.wms.model.PurchaseOrder;
import big_three.wms.model.PurchaseOrderLine;
import big_three.wms.repository.PurchaseOrderLineRepository;
import big_three.wms.repository.PurchaseOrderRepository;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                            PurchaseOrderLineRepository purchaseOrderLineRepository,
                            ProveedorRepository proveedorRepository,
                            ProductRepository productRepository,
                            ProductService productService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineRepository = purchaseOrderLineRepository;
        this.proveedorRepository = proveedorRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Transactional
    public PurchaseOrderResponseDTO create(PurchaseOrderCreateDTO dto) {
        if (!proveedorRepository.existsById(dto.getIdSupplier())) {
            throw new RuntimeException("Proveedor no encontrado");
        }
        for (PurchaseOrderLineCreateDTO line : dto.getLines()) {
            if (!productRepository.existsById(line.getIdProduct())) {
                throw new RuntimeException("Producto no encontrado: " + line.getIdProduct());
            }
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setIdSupplier(dto.getIdSupplier());
        order.setDateTime(LocalDateTime.now());
        order.setStatus(PurchaseOrder.Status.PENDIENTE);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        for (PurchaseOrderLineCreateDTO line : dto.getLines()) {
            PurchaseOrderLine orderLine = new PurchaseOrderLine();
            orderLine.setIdPurchaseOrder(savedOrder.getIdPurchaseOrder());
            orderLine.setIdProduct(line.getIdProduct());
            orderLine.setAmount(line.getAmount());
            purchaseOrderLineRepository.save(orderLine);
        }

        return buildResponse(savedOrder);
    }

    public List<PurchaseOrderResponseDTO> findAllSummaries() {
        return purchaseOrderRepository.findAll()
                .stream()
                .map(this::buildSummary)
                .collect(Collectors.toList());
    }

    public PurchaseOrderResponseDTO findById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada"));
        return buildResponse(purchaseOrder);
    }

    @Transactional
    public PurchaseOrderResponseDTO receive(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada"));

        if (purchaseOrder.getStatus() == PurchaseOrder.Status.RECIBIDA) {
            throw new IllegalArgumentException("La orden ya fue recibida");
        }
        if (purchaseOrder.getStatus() == PurchaseOrder.Status.CANCELADA) {
            throw new RuntimeException("No se puede recibir una orden cancelada");
        }

        for (PurchaseOrderLine orderLine : purchaseOrderLineRepository.findByIdPurchaseOrder(id)) {
            productService.ajustarStock(orderLine.getIdProduct(), orderLine.getAmount(), 0);
        }

        purchaseOrder.setStatus(PurchaseOrder.Status.RECIBIDA);
        purchaseOrderRepository.save(purchaseOrder);

        return buildResponse(purchaseOrder);
    }

    @Transactional
    public PurchaseOrderResponseDTO update(Long id, PurchaseOrderCreateDTO dto) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada"));

        if (!proveedorRepository.existsById(dto.getIdSupplier())) {
            throw new RuntimeException("Proveedor no encontrado");
        }
        for (PurchaseOrderLineCreateDTO line : dto.getLines()) {
            if (!productRepository.existsById(line.getIdProduct())) {
                throw new RuntimeException("Producto no encontrado: " + line.getIdProduct());
            }
        }

        purchaseOrder.setIdSupplier(dto.getIdSupplier());
        purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderLine> oldLines = purchaseOrderLineRepository.findByIdPurchaseOrder(id);
        boolean wasReceived = purchaseOrder.getStatus() == PurchaseOrder.Status.RECIBIDA;
        if (wasReceived) {
            for (PurchaseOrderLine oldLine : oldLines) {
                productService.ajustarStock(oldLine.getIdProduct(), -oldLine.getAmount(), 0);
            }
        }

        purchaseOrderLineRepository.deleteAll(oldLines);

        for (PurchaseOrderLineCreateDTO line : dto.getLines()) {
            PurchaseOrderLine orderLine = new PurchaseOrderLine();
            orderLine.setIdPurchaseOrder(id);
            orderLine.setIdProduct(line.getIdProduct());
            orderLine.setAmount(line.getAmount());
            purchaseOrderLineRepository.save(orderLine);
        }

        if (wasReceived) {
            for (PurchaseOrderLineCreateDTO line : dto.getLines()) {
                productService.ajustarStock(line.getIdProduct(), line.getAmount(), 0);
            }
        }

        return buildResponse(purchaseOrder);
    }

    @Transactional
    public void deleteById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada"));

        List<PurchaseOrderLine> lines = purchaseOrderLineRepository.findByIdPurchaseOrder(id);
        if (purchaseOrder.getStatus() == PurchaseOrder.Status.RECIBIDA) {
            for (PurchaseOrderLine line : lines) {
                productService.ajustarStock(line.getIdProduct(), -line.getAmount(), 0);
            }
        }

        purchaseOrderLineRepository.deleteAll(lines);
        purchaseOrderRepository.deleteById(id);
    }

    private PurchaseOrderResponseDTO buildResponse(PurchaseOrder purchaseOrder) {
        List<PurchaseOrderLine> lines = purchaseOrderLineRepository.findByIdPurchaseOrder(purchaseOrder.getIdPurchaseOrder());
        List<PurchaseOrderLineResponseDTO> orderLines = lines.stream()
                .map(l -> new PurchaseOrderLineResponseDTO(l.getIdProduct(), l.getAmount()))
                .collect(Collectors.toList());
        return new PurchaseOrderResponseDTO(
                purchaseOrder.getIdPurchaseOrder(),
                purchaseOrder.getDateTime(),
                purchaseOrder.getIdSupplier(),
                purchaseOrder.getStatus().name(),
                orderLines);
    }

    private PurchaseOrderResponseDTO buildSummary(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderResponseDTO(
                purchaseOrder.getIdPurchaseOrder(),
                purchaseOrder.getDateTime(),
                purchaseOrder.getIdSupplier(),
                purchaseOrder.getStatus().name(),
                null);
    }
}