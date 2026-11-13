package big_three.wms.repository;

import big_three.wms.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // Solo órdenes activas (fecha_baja IS NULL)
    List<PurchaseOrder> findByDeletedDateTimeIsNull();
}
