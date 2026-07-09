package big_three.wms.repository;

import big_three.wms.model.PickOrderLine;
import big_three.wms.model.PickOrderLine.PickOrderLineId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickOrderLineRepository extends JpaRepository<PickOrderLine, PickOrderLineId> {

    List<PickOrderLine> findByIdOrdenRetiro(Long idOrdenRetiro);

    List<PickOrderLine> findByIdProducto(Long idProducto);
}
