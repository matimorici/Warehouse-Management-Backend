package big_three.wms.service;

import big_three.wms.model.Proveedor;
import big_three.wms.model.PurchaseOrder;
import big_three.wms.dto.ProveedorCreateDTO;
import big_three.wms.dto.ProveedorResponseDTO;
import big_three.wms.repository.ProductRepository;
import big_three.wms.repository.ProveedorRepository;
import big_three.wms.repository.PurchaseOrderRepository;
import big_three.wms.security.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CurrentUserProvider currentUserProvider;

    public ProveedorService(ProveedorRepository proveedorRepository,
                            ProductRepository productRepository,
                            PurchaseOrderRepository purchaseOrderRepository,
                            CurrentUserProvider currentUserProvider) {
        this.proveedorRepository = proveedorRepository;
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public ProveedorResponseDTO create(ProveedorCreateDTO dto) {
        if (proveedorRepository.existsByCuit(dto.getCuit())) {
            throw new IllegalArgumentException(
                    "Ya existe un proveedor con ese CUIT (puede estar dado de baja): " + dto.getCuit());
        }
        Proveedor p = new Proveedor();
        p.setCuit(dto.getCuit());
        p.setRazonSocial(dto.getRazonSocial());
        p.setTelefono(dto.getTelefono());
        p.setMail(dto.getMail());
        p.setDireccion(dto.getDireccion());
        Proveedor saved = proveedorRepository.save(p);
        return convertToResponseDTO(saved);
    }

    public List<ProveedorResponseDTO> findAll() {
        return proveedorRepository.findByFechaBajaIsNull()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public ProveedorResponseDTO findById(Long id) {
        Proveedor p = proveedorRepository.findByIdProveedorAndFechaBajaIsNull(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        return convertToResponseDTO(p);
    }

    public ProveedorResponseDTO update(Long id, ProveedorCreateDTO dto) {
        Proveedor p = proveedorRepository.findByIdProveedorAndFechaBajaIsNull(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        if (!p.getCuit().equals(dto.getCuit()) && proveedorRepository.existsByCuit(dto.getCuit())) {
            throw new IllegalArgumentException(
                    "Ya existe un proveedor con ese CUIT (puede estar dado de baja): " + dto.getCuit());
        }
        p.setCuit(dto.getCuit());
        p.setRazonSocial(dto.getRazonSocial());
        p.setTelefono(dto.getTelefono());
        p.setMail(dto.getMail());
        p.setDireccion(dto.getDireccion());
        Proveedor updated = proveedorRepository.save(p);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public void deleteById(Long id) {
        Proveedor p = proveedorRepository.findByIdProveedorAndFechaBajaIsNull(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado para eliminar"));

        boolean hasActiveProducts = productRepository.existsByProveedor_IdProveedorAndFechaBajaIsNull(id);
        boolean hasPendingPurchaseOrders = purchaseOrderRepository
                .existsByIdSupplierAndStatusAndDeletedDateTimeIsNull(id, PurchaseOrder.Status.PENDIENTE);
        if (hasActiveProducts || hasPendingPurchaseOrders) {
            throw new IllegalArgumentException(
                    "No se puede dar de baja un proveedor con productos activos u órdenes pendientes");
        }

        p.setFechaBaja(LocalDateTime.now());
        p.setIdUsuarioBaja(currentUserProvider.requireCurrentUserId());
        proveedorRepository.save(p);
    }

    private ProveedorResponseDTO convertToResponseDTO(Proveedor proveedor) {
        ProveedorResponseDTO response = new ProveedorResponseDTO();
        response.setIdProveedor(proveedor.getIdProveedor());
        response.setCuit(proveedor.getCuit());
        response.setRazonSocial(proveedor.getRazonSocial());
        response.setTelefono(proveedor.getTelefono());
        response.setMail(proveedor.getMail());
        response.setDireccion(proveedor.getDireccion());
        return response;
    }
}
