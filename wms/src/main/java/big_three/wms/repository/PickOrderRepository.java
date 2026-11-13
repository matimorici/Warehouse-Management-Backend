package big_three.wms.repository;

import big_three.wms.model.PickOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickOrderRepository extends JpaRepository<PickOrder, Long> {

    // Solo órdenes activas (fecha_baja IS NULL)
    List<PickOrder> findByFechaBajaIsNull();
}
