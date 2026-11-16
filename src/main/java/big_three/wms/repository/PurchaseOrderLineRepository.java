package big_three.wms.repository;

import big_three.wms.model.PurchaseOrderLine;
import big_three.wms.model.PurchaseOrderLine.PurchaseOrderLineId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, PurchaseOrderLineId> {

    List<PurchaseOrderLine> findByIdPurchaseOrder(Long idPurchaseOrder);
}
